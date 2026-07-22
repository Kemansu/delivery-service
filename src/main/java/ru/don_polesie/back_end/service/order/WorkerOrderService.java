package ru.don_polesie.back_end.service.order;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.dto.order.request.ProcessQuantitiesDtoRequest;
import ru.don_polesie.back_end.exceptions.ConflictDataException;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.mapper.OrderMapper;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.model.order.OrderProduct;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.repository.OrderProductRepository;
import ru.don_polesie.back_end.repository.OrderRepository;

import ru.don_polesie.back_end.service.system.PriceService;
import ru.don_polesie.back_end.service.system.YooKassaService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkerOrderService {
    @Value("${utils.page-size}")
    private int PAGE_SIZE;

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderMapper orderMapper;
    private final YooKassaService yooKassaService;
    private final PriceService priceService;



    public OrderDtoResponse findById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Заказ не найден"));
        return orderMapper.toOrderDtoResponse(order);
    }

    /**
     * Получает страницу заказов
     *
     * @param pageNumber номер страницы (начинается с 1)
     * @return страница заказов в формате DTO
     */

    public Page<OrderDtoResponse> findOrdersPage(Integer pageNumber) {
        var pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
        return orderRepository.findAll(pageable)
                .map(orderMapper::toOrderDtoResponse);
    }

    /**
     * Получает страницу заказов со статусом RESERVAITED
     *
     * @param pageNumber номер страницы (начинается с 1)
     * @return страница заказов в формате DTO
     */

    public Page<OrderDtoResponse> findMoneyReservaitedOrdersPage(Integer pageNumber) {
        var pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
        return orderRepository.findAllMoneyReservaited(pageable)
                .map(orderMapper::toOrderDtoResponse);
    }

    /**
     * Отмечает заказ как отправленный
     *
     * @param id идентификатор заказа
     * @throws ObjectNotFoundException если заказ не найден
     */

    @Transactional
    public void markShipped(Long id) {
        Order order = getOrderById(id);
        if (!order.getStatus().equals(OrderStatus.READY_FOR_DELIVERY)) {
            throw new IllegalArgumentException("Заказ не был собран");
        }
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(@Min(value = 1) Long id, User user) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        if (!orderOptional.isPresent()) {
            throw new IllegalArgumentException("Заказа с таким id не существует");
        }
        Order order = orderOptional.get();
        if (!order.getStatus().equals(OrderStatus.MONEY_RESERVAITED)) {
            throw new IllegalArgumentException("Заказа еще не прошел оплату, его невозможно отменить");
        }
        order.setEmployee(user);
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }

    /**
     * Обрабатывает заказ: обновляет веса товаров и пересчитывает стоимость
     *
     * @param id идентификатор заказа
     * @param request запрос с весами для весовых товаров
     * @throws ObjectNotFoundException если заказ не найден
     * @throws IllegalArgumentException если не указан вес для весового товара
     */

    @Transactional
    public void processOrder(Long id, ProcessQuantitiesDtoRequest request) throws BadRequestException {
        Order order = getOrderById(id);
        if (!OrderStatus.ON_ASSEMBLY.equals(order.getStatus())) {
            throw new IllegalArgumentException("Заказ не собирался, заказ нельзя обработать");
        }
        Map<Long, ProcessQuantitiesDtoRequest.QuantityDto> quantitesMap = createQuantitesMap(request);
        BigDecimal newTotal = processOrderProducts(order, quantitesMap);
        updateOrderPayment(order, newTotal);

        orderRepository.save(order);
    }

    @Transactional
    public void takeOrder(@Min(value = 1) Long id, User user) {
        Optional<Order> orderOptional = orderRepository.findOrderByStatusAndEmployee(OrderStatus.ON_ASSEMBLY, user);
        if (orderOptional.isPresent()) {
            throw new ConflictDataException("У сотрудника уже есть заказ, который ждет обработки");
        }

        Order order = getOrderById(id);
        // PAID тоже допустим: платёж мог быть списан до сборки (капчер по вебхуку
        // или способ оплаты без холда) — иначе такой заказ зависал бы навсегда
        if (!OrderStatus.MONEY_RESERVAITED.equals(order.getStatus())
                && !OrderStatus.PAID.equals(order.getStatus())) {
            throw new IllegalArgumentException("Деньги не зарезервированы, заказ нельзя забрать для сборки");
        }
        order.setEmployee(user);
        order.setStatus(OrderStatus.ON_ASSEMBLY);
        orderRepository.save(order);
    }

    public void deleteProductFromOrder(Long orderId, Long productId) {
        Order order = getOrderById(orderId);
        if (!OrderStatus.MONEY_RESERVAITED.equals(order.getStatus())) {
            throw new IllegalArgumentException("Деньги не зарезервированы, заказ нельзя обработать");
        }

        for(OrderProduct orderProduct : order.getOrderProducts()) {
            if (orderProduct.getId().getProductId().equals(productId)) {
                orderProductRepository.delete(orderProduct);
                return;
            }
        }
        throw new IllegalArgumentException("Такого товара нет в заказе");
    }

    /**
     * Получает общее количество продаж товара за указанную дату
     *
     * @param productId идентификатор товара
     * @param date дата для анализа продаж
     * @return общее количество проданного товара
     */

    public Long getTotalSalesForProductByDate(Long productId, Instant date) {
        return orderRepository.getTotalSalesForProductByDate(productId, date);
    }

    /**
     * Получает количество заказов с указанным товаром за дату
     *
     * @param productId идентификатор товара
     * @param date дата для анализа
     * @return количество заказов
     */

    public Long getOrderCountForProductByDate(Long productId, Instant date) {
        return orderRepository.getOrderCountForProductByDate(productId, date);
    }


    public Page<OrderDtoResponse> findOrdersPageWithStatus(Integer pageNumber, OrderStatus orderStatus) {
        var pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
        return orderRepository.findAllByStatus(orderStatus, pageable)
                .map(orderMapper::toOrderDtoResponse);
    }

    public OrderDtoResponse findAssemblyOrderForEmployee(User user) {
        Optional<Order> order = orderRepository.findOrderByStatusAndEmployee(OrderStatus.ON_ASSEMBLY, user);
        if (order.isEmpty()) {
            throw new ObjectNotFoundException("Нет заказов на этапе сборки для сотрудника");
        }
        return orderMapper.toOrderDtoResponse(order.get());
    }

    // ========== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Находит заказ по ID или выбрасывает исключение
     *
     * @param id идентификатор заказа
     * @return найденный заказ
     * @throws ObjectNotFoundException если заказ не найден
     */
    private Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Order not found with id: " + id));
    }

    /**
     * Создает карту весов товаров для быстрого доступа
     *
     * @param request запрос с количеством каждого товара
     * @return карта: productId -> QuantityDto
     */
    private Map<Long, ProcessQuantitiesDtoRequest.QuantityDto> createQuantitesMap(ProcessQuantitiesDtoRequest request) {
        return request.getQuantities().stream()
                .collect(
                        Collectors.toMap(
                                ProcessQuantitiesDtoRequest.QuantityDto::getProductId, quantity -> quantity
                ));
    }

    /**
     * Обрабатывает все товары в заказе и вычисляет новую общую стоимость
     *
     * @param order заказ для обработки
     * @return новая общая стоимость заказа
     */
    private BigDecimal processOrderProducts(Order order, Map<Long, ProcessQuantitiesDtoRequest.QuantityDto> quantityDtoMap) throws BadRequestException {
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            if (!quantityDtoMap.containsKey(orderProduct.getProduct().getId())) {
                throw new BadRequestException("Не указано число товара: " + orderProduct.getProduct().getName());
            } else if (quantityDtoMap.get(orderProduct.getProduct().getId()).getQuantity() > orderProduct.getQuantity()) {
                throw new BadRequestException("Число товаров не может превышать изначального: " + orderProduct.getProduct().getName());
            } else if (quantityDtoMap.get(orderProduct.getProduct().getId()).getQuantity() == 0) {
                orderProductRepository.delete(orderProduct);
            }
        }
        return order.getOrderProducts().stream()
                .map(orderProduct -> processOrderProduct(orderProduct, quantityDtoMap))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Обрабатывает один товар в заказе: обновляет количество и рассчитывает стоимость
     *
     * @param orderProduct товар в заказе
     * @param quantityDtoMapMap карта количества каждого товара
     * @return стоимость товара после обработки
     */
    private BigDecimal processOrderProduct(OrderProduct orderProduct, Map<Long, ProcessQuantitiesDtoRequest.QuantityDto> quantityDtoMapMap) {
        updateOrderProductQuantity(orderProduct, quantityDtoMapMap.get(orderProduct.getProduct().getId()).getQuantity());
        return calculateProductCost(orderProduct, quantityDtoMapMap.get(orderProduct.getProduct().getId()).getQuantity());
    }


    /**
     * Обновляет количество товара в заказе
     *
     * @param orderProduct товар в заказе
     * @param quantity новое количество
     */
    private void updateOrderProductQuantity(OrderProduct orderProduct, int quantity) {
        orderProduct.setQuantity(quantity);
        orderProductRepository.save(orderProduct);
    }

    /**
     * Рассчитывает стоимость товара
     *
     * @param orderProduct товар в заказе
     * @param quantity количество товара
     * @return стоимость товара
     */
    private BigDecimal calculateProductCost(OrderProduct orderProduct, int quantity) {
        return priceService.calculateProductCost(orderProduct.getProduct(), quantity);
    }

    /**
     * Обновляет платеж заказа: устанавливает новую сумму и обрабатывает платеж
     *
     * @param order заказ для обновления
     * @param newTotal новая общая стоимость заказа
     */
    private void updateOrderPayment(Order order, BigDecimal newTotal) {
        order.setTotalAmount(newTotal);
        try {
            // Если деньги уже списаны (заказ пришёл в сборку из PAID) — повторный
            // капчер вернул бы ошибку и заказ ушёл бы в ERROR_ON_PAYMENT
            if (yooKassaService.isCaptured(order)) {
                log.info("Оплата по заказу {} уже списана — капчер пропущен", order.getId());
            } else {
                yooKassaService.getMoney(order);
            }
            order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        } catch (Exception e) {
            order.setStatus(OrderStatus.ERROR_ON_PAYMENT);
        }
    }
}