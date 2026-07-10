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
import ru.don_polesie.back_end.dto.address.response.AddressDtoResponse;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.dto.order.response.OrderCreatedDtoResponse;
import ru.don_polesie.back_end.exceptions.ConflictDataException;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.model.basket.BasketProduct;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.mapper.AddressMapper;
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
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
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


    @Deprecated
    @Transactional
    public OrderCreatedDtoResponse changeQuantityProductFromOrder(Long orderId, Long productId, int quantity) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.PAYING) {
            throw new RuntimeException("Невозможно редактировать заказ.");
        }
        Product product = getProductById(productId);
        OrderProduct orderProduct = getOrderProduct(orderId, productId);
        orderProduct.setQuantity(quantity);

        log.info("Changing quantity product {} from order {}", product, order);
        BigDecimal productPrice = priceService.calculateProductCost(product, quantity);
        order.setTotalAmount(order.getTotalAmount().subtract(order.getTotalAmount().subtract(productPrice)));
        order.removeProduct(orderProduct);
        order.addProduct(orderProduct);
        log.info("New order price is {}", order.getTotalAmount());
        orderRepository.save(order);
        return createPaymentResponse(order);
    }

    /**
     * Удаляет товар из заказа и пересчитывает стоимость
     *
     * @param orderId идентификатор заказа
     * @param productId идентификатор товара
     * @throws ObjectNotFoundException если заказ, товар или связка не найдены
     */
    @Deprecated
    @Transactional
    public OrderCreatedDtoResponse deleteProductFromOrder(Long orderId, Long productId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.MONEY_RESERVAITED) {
            throw new RuntimeException("Невозможно редактировать заказ.");
        }
        Product product = getProductById(productId);
        OrderProduct orderProduct = getOrderProduct(orderId, productId);

        log.info("Deleting product {} from order {}", product, order);
        BigDecimal productPrice = priceService.calculateProductCost(product, orderProduct.getQuantity());
        order.setTotalAmount(order.getTotalAmount().subtract(productPrice));
        order.removeProduct(orderProduct);
        log.info("New order price is {}", order.getTotalAmount());
        orderRepository.save(order);
        return createPaymentResponse(order);
    }


    // ========== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    /**
     * Создает объект пагинации
     */
    private Pageable createPageable(Integer pageNumber) {
        return PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
    }

    /**
     * Обрабатывает адрес доставки
     */
    private Address processAddress(AddressDtoResponse addressDto) {
        Address address = addressMapper.toEntity(addressDto);
        return findExistingAddressOrSaveNew(address);
    }

    /**
     * Находит существующий адрес или сохраняет новый
     */
    private Address findExistingAddressOrSaveNew(Address address) {
        if (address.getId() == null) {
            return addressRepository.save(address);
        }
        return addressRepository.findById(address.getId())
                .orElseGet(() -> addressRepository.save(address));
    }

    /**
     * Создает новый заказ
     */
    private Order createOrder(OrderDtoResponse orderDtoResponse, User user, Address address) {
        Order order = orderMapper.toOrder(orderDtoResponse);
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    /**
     * Обрабатывает товары в заказе и рассчитывает общую стоимость
     */
    private void processOrderItems(OrderDtoResponse orderDtoResponse, Order order) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderProduct> orderProducts = new ArrayList<>();

        for (OrderDtoResponse.OrderItemDto itemDto : orderDtoResponse.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));

            // Создаем связь OrderProduct
            OrderProduct orderProduct = new OrderProduct();

            // Убедитесь, что OrderProductId создается правильно
            OrderProductId orderProductId = new OrderProductId();
            orderProductId.setOrderId(order.getId()); // Теперь order.getId() не null
            orderProductId.setProductId(product.getId());
            orderProduct.setId(orderProductId);

            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(itemDto.getQuantity());

            orderProducts.add(orderProduct);

            // Рассчитываем стоимость
            BigDecimal itemCost = priceService.calculateProductCost(product, itemDto.getQuantity());
            totalAmount = totalAmount.add(itemCost);
        }

        // Сохраняем все OrderProducts
        orderProductRepository.saveAll(orderProducts);

        // Устанавливаем связи и обновляем сумму
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PAYING);
    }

    /**
     * Обрабатывает один товар в заказе
     */
    private BigDecimal processOrderItem(OrderDtoResponse.OrderItemDto orderItem, Order order) {
        Product product = getProductById(orderItem.getProductId());
        OrderProduct orderProduct = new OrderProduct(order, product, orderItem.getQuantity());

        BigDecimal itemCost = priceService.calculateProductCost(product, orderItem.getQuantity());
        order.addProduct(orderProduct);

        return itemCost;
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

    /**
     * Находит заказ по ID
     */
    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ObjectNotFoundException("Order not found with id: " + orderId));
    }

    /**
     * Находит товар по ID
     */
    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ObjectNotFoundException("Product not found with id: " + productId));
    }

    /**
     * Находит связку товара в заказе
     */
    private OrderProduct getOrderProduct(Long orderId, Long productId) {
        OrderProductId orderProductId = new OrderProductId(orderId, productId);
        return orderProductRepository.findById(orderProductId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Order product not found for orderId: " + orderId + " and productId: " + productId));
    }
}