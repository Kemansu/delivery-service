package ru.don_polesie.back_end.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.model.Role;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.model.order.OrderProduct;
import ru.don_polesie.back_end.model.order.OrderProductId;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.repository.*;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Log4j2
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Создание дефолтных аккаунтов с известными паролями — только по явному флагу
    // (для локальной разработки). На проде флаг НЕ задан → сид не выполняется,
    // общеизвестные креды admin123/user123 не заводятся и не логируются.
    @Value("${security.seed-default-users:false}")
    private boolean seedDefaultUsers;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing data...");
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        Role workerRole = roleRepository.findByName("ROLE_WORKER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_WORKER")));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        log.info("Roles:");
        roleRepository.findAll().forEach(role -> {
            System.out.println(role.getName());
        });

        // Дефолтные аккаунты с известными паролями заводятся ТОЛЬКО в dev
        // (security.seed-default-users=true). На проде блок не выполняется.
        if (seedDefaultUsers) {
            User admin = new User();
            if (userRepository.findByPhoneNumber("79919916871").isEmpty()) {
                admin.setName("Денис");
                admin.setSurname("Погосов");
                admin.setPhoneNumber("79919916871");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.getRoles().add(adminRole);
                admin.getRoles().add(workerRole);
                admin.getRoles().add(userRole);
                userRepository.save(admin);
            }

            User user = new User();
            Address address = new Address();
            if (userRepository.findByPhoneNumber("79889995000").isEmpty()) {
                user.setName("Виктор");
                user.setSurname("Николаенко");
                user.setPhoneNumber("79889995000");
                user.setEmail("viktor.nikolaenko.2005@gmail.com");
                user.setPassword(passwordEncoder.encode("user123"));
                user.getRoles().add(userRole);
                userRepository.save(user);

                address.setActive(true);
                address.setCity("Ростов-на-Дону");
                address.setStreet("Сержантова");
                address.setFloor(21);
                address.setHouseNumber("9/27");
                address.setApartmentNumber(190);
                address.setUser(user);
                addressRepository.save(address);
            }
            log.info("Dev seed users created (security.seed-default-users=true)");
        }

        /*
        if (orderRepository.findById(1L).isEmpty()){
            // 2. Создаем заказ
            Order order = new Order();
            order.setUser(user);
            order.setStatus(OrderStatus.SHIPPED);
            order.setEmployee(admin);
            order.setPaymentId("test-payment-id");
            order.setTotalAmount(new BigDecimal("100.00"));
            order.setPhoneNumber(user.getPhoneNumber()); // используем phoneNumber из user
            order.setAddress(address);

            // 3. Создаем OrderProduct и связываем его с заказом
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setProduct(productRepository.findAll().getFirst());
            orderProduct.setQuantity(1);

            // 4. Создаем и устанавливаем ID для OrderProduct
            OrderProductId orderProductId = new OrderProductId();
            orderProduct.setId(orderProductId);

            // 5. Добавляем продукт в заказ (это установит двустороннюю связь)
            order.addProduct(orderProduct);

            // 6. Сохраняем заказ (cascade = CascadeType.ALL сохранит и orderProducts)
            orderRepository.save(order);
        }
         */
    }

    private void addData() {

        // Создаем категории
        Category sausageCategory = new Category();
        sausageCategory.setName("Колбасы");
        sausageCategory.setActive(true);

        Category cheeseCategory = new Category();
        cheeseCategory.setName("Сыры");
        cheeseCategory.setActive(true);

        Category milkCategory = new Category();
        milkCategory.setName("Молочные продукты");
        milkCategory.setActive(true);

        // Создаем бренды
        Brand vyazankaBrand = new Brand();
        vyazankaBrand.setName("Вязанка");
        vyazankaBrand.setActive(true);

        Brand paramiBrand = new Brand();
        paramiBrand.setName("ПАРМИЧАТА");
        paramiBrand.setActive(true);

        Brand prostokvashinoBrand = new Brand();
        prostokvashinoBrand.setName("Простоквашино");
        prostokvashinoBrand.setActive(true);

        // Создаем продукты

        // 1. Сервелат "Вязанка"
        Product product1 = Product.builder()
                .name("Сервелат Вязанка особый")
                .price(new BigDecimal("489.99"))
                .imageUrl("https://abi.ru/upload/iblock/1b9/hgw59ke7n5ae9fu0d7husi1taduebbkx.png")
                .fatGrams(new BigDecimal("35.50"))
                .proteinGrams(new BigDecimal("12.80"))
                .carbohydrateGrams(new BigDecimal("2.10"))
                .energyKcalPer100g(new BigDecimal("345.75"))
                .brand(vyazankaBrand)
                .category(sausageCategory)
                .minWeight(300)
                .maxWeight(500)
                .amount(150)
                .storageTemperatureMin(0)
                .storageTemperatureMax(6)
                .isWeighted(true)
                .countryOfOrigin("Россия")
                .sale(15)
                .active(true)
                .composition("Говядина высшего сорта, свинина, нитритная соль, молочный белок, " +
                        "специи (перец черный, перец душистый, мускатный орех, чеснок), " +
                        "сахар, антиокислитель Е316, стабилизатор Е450, краситель Е120.")
                .description("Превосходный сырокопченый сервелат с насыщенным вкусом и ароматом. " +
                        "Изготовлен по традиционному рецепту с использованием отборного мяса и натуральных специй. " +
                        "Идеально подходит для праздничного стола, бутербродов и нарезки.")
                .shelfLife("30 суток")
                .build();

// 2. Колбаса сырокопченая "Вязанка"
        Product product2 = Product.builder()
                .name("Колбаса сырокопченая Вязанка Люкс")
                .price(new BigDecimal("689.50"))
                .imageUrl("https://tornado.shop/images/detailed/13/P1174349.jpg")
                .fatGrams(new BigDecimal("42.30"))
                .proteinGrams(new BigDecimal("15.20"))
                .carbohydrateGrams(new BigDecimal("1.80"))
                .energyKcalPer100g(new BigDecimal("412.50"))
                .brand(vyazankaBrand)
                .category(sausageCategory)
                .minWeight(250)
                .maxWeight(400)
                .amount(95)
                .storageTemperatureMin(0)
                .storageTemperatureMax(8)
                .isWeighted(true)
                .countryOfOrigin("Россия")
                .sale(0)
                .active(true)
                .composition("Свинина, говядина, шпик, соль нитритная, пряности (перец черный, перец белый, " +
                        "мускатный орех, кардамон, кориандр), сахар, регулятор кислотности Е262, " +
                        "антиокислитель Е300, натуральная оболочка.")
                .description("Элитная сырокопченая колбаса высшего сорта. Длительный процесс холодного копчения " +
                        "придает продукту неповторимый аромат и золотистый цвет. Мягкая консистенция " +
                        "и сбалансированный вкус с нотами натуральных специй.")
                .shelfLife("45 суток")
                .build();

// 3. Сыр "ПАРМИЧАТА" твердый
        Product product3 = Product.builder()
                .name("Сыр ПАРМИЧАТА Премиум 45%")
                .price(new BigDecimal("329.99"))
                .imageUrl("https://ir.ozone.ru/s3/multimedia-1-m/w500/7209409306.jpg")
                .fatGrams(new BigDecimal("28.50"))
                .proteinGrams(new BigDecimal("25.00"))
                .carbohydrateGrams(new BigDecimal("0.00"))
                .energyKcalPer100g(new BigDecimal("356.00"))
                .brand(paramiBrand)
                .category(cheeseCategory)
                .minWeight(200)
                .maxWeight(1000)
                .amount(200)
                .storageTemperatureMin(2)
                .storageTemperatureMax(6)
                .isWeighted(true)
                .countryOfOrigin("Россия")
                .sale(20)
                .active(true)
                .composition("Пастеризованное молоко, закваска молочнокислых культур, ферментный препарат " +
                        "микробного происхождения, соль пищевая, хлорид кальция, краситель натуральный Е160b.")
                .description("Твердый сыр с жирностью 45%. Обладает нежным сливочным вкусом с легкой ореховой ноткой. " +
                        "Отлично плавится, идеален для фондю, пиццы, горячих бутербродов. Созревает 60 дней, " +
                        "что придает сыру плотную структуру и насыщенный вкус.")
                .shelfLife("90 суток")
                .build();

// 4. Молоко "Простоквашино"
        Product product4 = Product.builder()
                .name("Молоко Простоквашино ультрапастеризованное 3.2%")
                .price(new BigDecimal("89.90"))
                .imageUrl("https://tsx.x5static.net/i/800x800-fit/xdelivery/files/23/22/e01xd51b61fc693543c5d9d85b94.jpg")
                .fatGrams(new BigDecimal("3.20"))
                .proteinGrams(new BigDecimal("3.00"))
                .carbohydrateGrams(new BigDecimal("4.70"))
                .energyKcalPer100g(new BigDecimal("64.50"))
                .brand(prostokvashinoBrand)
                .category(milkCategory)
                .minWeight(1000) // 1 литр в граммах
                .maxWeight(1000)
                .amount(300)
                .storageTemperatureMin(2)
                .storageTemperatureMax(6)
                .isWeighted(false) // Фасованный товар
                .countryOfOrigin("Россия")
                .sale(10)
                .active(true)
                .composition("Молоко ультрапастеризованное 3.2% жирности. Без добавления консервантов.")
                .description("Ультрапастеризованное молоко с классической жирностью 3.2%. Обработка при высокой " +
                        "температуре позволяет сохранить все полезные свойства молока без использования " +
                        "консервантов. Имеет натуральный сливочный вкус и аромат свежего молока.")
                .shelfLife("10 суток")
                .build();

// 5. Сметана "Простоквашино"
        Product product5 = Product.builder()
                .name("Сметана Простоквашино 20%")
                .price(new BigDecimal("65.50"))
                .imageUrl("https://prostokvashino.ru/upload/resize_cache/iblock/5b3/640_640_0/p2exid3k9wtrazm9yv2bdj198bmclynn.png")
                .fatGrams(new BigDecimal("20.00"))
                .proteinGrams(new BigDecimal("2.80"))
                .carbohydrateGrams(new BigDecimal("3.20"))
                .energyKcalPer100g(new BigDecimal("206.00"))
                .brand(prostokvashinoBrand)
                .category(milkCategory)
                .minWeight(400)
                .maxWeight(400)
                .amount(250)
                .storageTemperatureMin(2)
                .storageTemperatureMax(6)
                .isWeighted(false) // Фасованный товар
                .countryOfOrigin("Россия")
                .sale(0)
                .active(true)
                .composition("Нормализованные сливки, закваска молочнокислых культур. " +
                        "Жирность 20%. Без растительных жиров.")
                .description("Классическая сметана с нежной консистенцией и сбалансированным вкусом. " +
                        "Изготавливается из отборных сливок с использованием натуральной закваски. " +
                        "Идеально подходит для заправки супов, салатов, приготовления соусов и выпечки.")
                .shelfLife("14 суток")
                .build();

// 6. Колбаса вареная "Вязанка" (невесовой вариант)
        Product product6 = Product.builder()
                .name("Колбаса вареная Вязанка Докторская")
                .price(new BigDecimal("259.99"))
                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSJqbFY_SWkF9mlMk05FUtDltaGz4uVIixq0A&s")
                .fatGrams(new BigDecimal("22.50"))
                .proteinGrams(new BigDecimal("11.30"))
                .carbohydrateGrams(new BigDecimal("3.50"))
                .energyKcalPer100g(new BigDecimal("240.25"))
                .brand(vyazankaBrand)
                .category(sausageCategory)
                .minWeight(350) // Фасовка по 350г
                .maxWeight(350)
                .amount(180)
                .storageTemperatureMin(0)
                .storageTemperatureMax(5)
                .isWeighted(false) // Фасованный товар
                .countryOfOrigin("Россия")
                .sale(25)
                .active(true)
                .composition("Говядина, свинина, молоко сухое, яичный порошок, соль, сахар, " +
                        "экстракты специй (перец черный, мускатный орех, кардамон), " +
                        "стабилизатор Е450, антиокислитель Е300, фиксатор окраски Е250.")
                .description("Колбаса вареная высшего сорта «Докторская». Традиционный рецепт с 1936 года. " +
                        "Нежный вкус и однородная консистенция. Отлично подходит для детского питания, " +
                        "бутербродов и салатов. Упакована в современную барьерную пленку для сохранения свежести.")
                .shelfLife("15 суток")
                .build();
        brandRepository.save(paramiBrand);
        brandRepository.save(prostokvashinoBrand);
        brandRepository.save(vyazankaBrand);

        categoryRepository.save(sausageCategory);
        categoryRepository.save(milkCategory);
        categoryRepository.save(cheeseCategory);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
        productRepository.save(product4);
        productRepository.save(product5);
        productRepository.save(product6);
    }
}
