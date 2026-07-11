package ru.don_polesie.back_end.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.dto.order.response.OrderCreatedDtoResponse;
import ru.don_polesie.back_end.exceptions.ConflictDataException;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.model.basket.BasketProduct;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.mapper.OrderMapper;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.model.order.OrderProduct;
import ru.don_polesie.back_end.model.order.OrderProductId;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.repository.*;
import ru.don_polesie.back_end.service.system.PriceService;
import ru.don_polesie.back_end.service.system.YooKassaService;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOrderService {

    @Value("${utils.page-size}")
    private int PAGE_SIZE;

    @Value("${utils.min-price-for-delivery}")
    private int MIN_PRICE;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AddressRepository addressRepository;
    private final YooKassaService yooKassaServiceImpl;
    private final PriceService priceService;
    private final BasketRepository basketRepository;

    /**
     * Находит заказ по идентификатору
     *
     * @param id идентификатор заказа
     * @return DTO заказа
     * @throws ObjectNotFoundException если заказ не найден
     */

    public OrderDtoResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toOrderDtoResponse)
                .orElseThrow(() -> new ObjectNotFoundException("Order not found with id: " + id));
    }

    /**
     * Получает страницу заказов пользователя
     *
     * @param pageNumber номер страницы
     * @param phoneNumber номер пользователя
     * @return страница с заказами
     */

    public Page<OrderDtoResponse> findUserOrdersPage(Integer pageNumber, String phoneNumber) {
        Pageable pageable = createPageable(pageNumber);
        return orderRepository.findByUserPhoneNumber(phoneNumber, pageable)
                .map(orderMapper::toOrderDtoResponse);
    }

    /**
     * Получает страницу доставленных заказов пользователя
     *
     * @param pageNumber номер страницы
     * @param phoneNumber номер пользователя
     * @return страница с доставленными заказами
     */

    public Page<OrderDtoResponse> findShippedUserOrdersPage(Integer pageNumber, String phoneNumber) {
        Pageable pageable = createPageable(pageNumber);
        return orderRepository.findByUserPhoneNumberAndStatus(phoneNumber, OrderStatus.SHIPPED, pageable)
                .map(orderMapper::toOrderDtoResponse);
    }

    /**
     * Удаляет заказ по идентификатору
     *
     * @param orderId идентификатор заказа
     */

    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("Deleting order with id: {}", orderId);
        orderRepository.deleteById(orderId);
    }


    /**
     * Создает новый заказ и платеж на основе корзины пользователя
     *
     * @param user пользователь, создающий заказ
     * @return ответ с созданным заказом и данными платежа
     * @throws RuntimeException если не удалось создать платеж
     */
    @Transactional
    public OrderCreatedDtoResponse save(User user, Long addressId, Boolean needCallForApproval) {
        Optional<Address> address = addressRepository.findById(addressId);
        if (address.isEmpty()) {
            throw new ObjectNotFoundException("Address not found with id: " + addressId);
        }

        log.info("Saving order for user: {}", user.getPhoneNumber());

        // Получаем корзину пользователя
        Basket basket = basketRepository.findByUser_PhoneNumber(user.getPhoneNumber())
                .orElseThrow(() -> new ObjectNotFoundException("Basket not found for user: " + user.getPhoneNumber()));

        if (basket.getBasketProducts().isEmpty()) {
            throw new ObjectNotFoundException("Корзина пустая, невозможно создать заказ.");
        }

        // Создаем заказ
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address.get());
        order.setStatus(OrderStatus.PAYING);
        order.setPhoneNumber(order.getUser().getPhoneNumber());
        order.setNeedCallForApproval(needCallForApproval);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Переносим все товары из корзины в заказ
        Set<OrderProduct> orderProducts = new HashSet<>();
        for (BasketProduct basketProduct : basket.getBasketProducts()) {
            Product product = basketProduct.getProduct();
            int quantity = basketProduct.getQuantity();

            BigDecimal itemCost = priceService.calculateProductCost(product, quantity);
            totalAmount = totalAmount.add(itemCost);

            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(quantity);

            orderProduct.setId(new OrderProductId(
                    order.getId(),
                    basketProduct.getProduct().getId()
            ));

            orderProducts.add(orderProduct);
        }
        if (totalAmount.compareTo(BigDecimal.valueOf(MIN_PRICE)) < 0) {
            throw new ConflictDataException("Сумма заказа меньше минимальной, закажите товаров на " + (MIN_PRICE - totalAmount.doubleValue()) + "р");
        }
        order.setTotalAmount(totalAmount);
        order.getOrderProducts().addAll(orderProducts);

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created with total amount {}", savedOrder.getId(), totalAmount);

        // Очищаем корзину после оформления заказа
        basket.getBasketProducts().clear();
        basket.setTotalAmount(BigDecimal.ZERO);
        basketRepository.save(basket);

        // Создаем платеж
        return createPaymentResponse(savedOrder);
    }


    // ========== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    /**
     * Создает объект пагинации
     */
    private Pageable createPageable(Integer pageNumber) {
        return PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
    }

    /**
     * Создает ответ с данными платежа
     */
    private OrderCreatedDtoResponse createPaymentResponse(Order order) {
        try {
            return new OrderCreatedDtoResponse(
                    orderMapper.toOrderDtoResponse(order),
                    yooKassaServiceImpl.createPayment(order.getId())
            );
        } catch (Exception e) {
            throw new RuntimeException("Payment creation failed: " + e.getMessage());
        }
    }
}