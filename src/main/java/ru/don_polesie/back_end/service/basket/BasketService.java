package ru.don_polesie.back_end.service.basket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.model.basket.BasketProduct;
import ru.don_polesie.back_end.model.basket.BasketProductId;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.repository.BasketProductRepository;
import ru.don_polesie.back_end.repository.BasketRepository;
import ru.don_polesie.back_end.repository.ProductRepository;
import ru.don_polesie.back_end.repository.UserRepository;
import ru.don_polesie.back_end.service.system.PriceService;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketProductRepository basketProductRepository;
    private final PriceService priceService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Basket getBasket(String phoneNumber) {
        return getBasketByUserPhoneNumber(phoneNumber);
    }

    /**
     * Добавление товара в корзину пользователя по номеру телефона.
     */
    @Transactional
    public Basket addProduct(String phoneNumber, Long productId, int quantity) {
        Basket basket = getBasketByUserPhoneNumber(phoneNumber);
        Product product = getProductById(productId);

        // Получаем или создаем связь BasketProduct
        BasketProduct basketProduct = basketProductRepository
                .findById(new BasketProductId(basket.getId(), productId))
                .orElseGet(() -> new BasketProduct(basket, product, 0));

        basketProduct.setQuantity(basketProduct.getQuantity() + quantity);

        log.info("Add product {} (quantity: {}) to basket {}", product.getName(), quantity, basket.getId());

        BigDecimal productCost = priceService.calculateProductCost(product, basketProduct.getQuantity());
        basket.setTotalAmount(basket.getTotalAmount().add(productCost));

        basket.addProduct(basketProduct);
        basketRepository.save(basket);

        log.info("New basket total amount: {}", basket.getTotalAmount());
        return basket;
    }

    /**
     * Изменяет количество конкретного товара в корзине.
     */
    @Transactional
    public Basket changeQuantityProduct(String phoneNumber, Long productId, int quantity) {
        Basket basket = getBasketByUserPhoneNumber(phoneNumber);
        Product product = getProductById(productId);
        BasketProduct basketProduct = getBasketProduct(basket.getId(), productId);

        log.info("Changing quantity of product {} in basket {}", product.getName(), basket.getId());

        // Пересчет стоимости корзины
        BigDecimal oldProductCost = priceService.calculateProductCost(product, basketProduct.getQuantity());
        BigDecimal newProductCost = priceService.calculateProductCost(product, quantity);

        basketProduct.setQuantity(quantity);
        basket.setTotalAmount(basket.getTotalAmount().subtract(oldProductCost).add(newProductCost));

        basketRepository.save(basket);
        log.info("New basket total amount: {}", basket.getTotalAmount());
        return basket;
    }

    /**
     * Удаляет товар из корзины и пересчитывает стоимость.
     */
    @Transactional
    public Basket deleteProductFromBasket(String phoneNumber, Long productId) {
        Basket basket = getBasketByUserPhoneNumber(phoneNumber);
        Product product = getProductById(productId);
        BasketProduct basketProduct = getBasketProduct(basket.getId(), productId);

        log.info("Deleting product {} from basket {}", product.getName(), basket.getId());

        BigDecimal productCost = priceService.calculateProductCost(product, basketProduct.getQuantity());
        basket.setTotalAmount(basket.getTotalAmount().subtract(productCost));

        basket.removeProduct(basketProduct);
        basketProductRepository.delete(basketProduct);

        basketRepository.save(basket);
        log.info("New basket total amount: {}", basket.getTotalAmount());
        return basket;
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    private Basket getBasketByUserPhoneNumber(String phoneNumber) {
        Optional<Basket> userBasket = basketRepository.findByUser_PhoneNumber(phoneNumber);
        if (userBasket.isEmpty()) {
            Basket basket = new Basket();
            basket.setUser(userRepository.findByPhoneNumber(phoneNumber).get());
            return basketRepository.save(basket);
        }
        return userBasket.get();
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ObjectNotFoundException("Product not found with id: " + productId));
    }

    private BasketProduct getBasketProduct(Long basketId, Long productId) {
        BasketProductId basketProductId = new BasketProductId(basketId, productId);
        return basketProductRepository.findById(basketProductId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Basket product not found for basketId: " + basketId + " and productId: " + productId));
    }

}

