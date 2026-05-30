package ru.don_polesie.back_end.model.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    // Константы для валидации
    private static final int MIN_TEMPERATURE = -100;
    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_WEIGHT = 1;
    private static final int MAX_WEIGHT = 50000; // 50 кг в граммах
    private static final int MIN_AMOUNT = 0;
    private static final int MAX_AMOUNT = 100000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @Column(unique = true, nullable = false, length = 200)
    @NotBlank(message = "Название товара обязательно")
    @Size(max = 200, message = "Название товара не может превышать 200 символов")
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть не менее 0.01")
    @DecimalMax(value = "1000000.00", message = "Цена не может превышать 1,000,000.00")
    private BigDecimal price;

    @Column(name = "image_url", nullable = false, length = 500)
    @NotBlank(message = "URL изображения обязателен")
    @URL(message = "URL изображения должен быть валидным")
    @Size(max = 500, message = "URL изображения не может превышать 500 символов")
    private String imageUrl;

    // Пищевая ценность на 100г
    @Column(name = "fat_grams", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Содержание жиров обязательно")
    @DecimalMin(value = "0.00", message = "Содержание жиров не может быть отрицательным")
    @DecimalMax(value = "100.00", message = "Содержание жиров не может превышать 100г")
    private BigDecimal fatGrams;

    @Column(name = "protein_grams", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Содержание белков обязательно")
    @DecimalMin(value = "0.00", message = "Содержание белков не может быть отрицательным")
    @DecimalMax(value = "100.00", message = "Содержание белков не может превышать 100г")
    private BigDecimal proteinGrams;

    @Column(name = "carbohydrate_grams", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Содержание углеводов обязательно")
    @DecimalMin(value = "0.00", message = "Содержание углеводов не может быть отрицательным")
    @DecimalMax(value = "100.00", message = "Содержание углеводов не может превышать 100г")
    private BigDecimal carbohydrateGrams;

    @Column(name = "energy_kcal_per_100g", nullable = false, precision = 6, scale = 2)
    @NotNull(message = "Энергетическая ценность обязательна")
    @DecimalMin(value = "0.00", message = "Энергетическая ценность не может быть отрицательной")
    @DecimalMax(value = "1000.00", message = "Энергетическая ценность не может превышать 1000 ккал")
    private BigDecimal energyKcalPer100g;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @NotNull(message = "Бренд обязателен")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @NotNull(message = "Категория обязательна")
    private Category category;

    @Column(name = "min_weight")
    @Min(value = MIN_WEIGHT, message = "Минимальный вес должен быть не менее " + MIN_WEIGHT + "г")
    @Max(value = MAX_WEIGHT, message = "Минимальный вес не может превышать " + MAX_WEIGHT + "г")
    private Integer minWeight;

    @Column(name = "max_weight", nullable = false)
    @NotNull(message = "Максимальный вес обязателен")
    @Min(value = MIN_WEIGHT, message = "Максимальный вес должен быть не менее " + MIN_WEIGHT + "г")
    @Max(value = MAX_WEIGHT, message = "Максимальный вес не может превышать " + MAX_WEIGHT + "г")
    private Integer maxWeight;

    @Column(nullable = false)
    @NotNull(message = "Количество обязательно")
    @Min(value = MIN_AMOUNT, message = "Количество не может быть отрицательным")
    @Max(value = MAX_AMOUNT, message = "Количество не может превышать " + MAX_AMOUNT)
    private Integer amount;

    @Column(name = "storage_temperature_min", nullable = false)
    @NotNull(message = "Минимальная температура хранения обязательна")
    @Min(value = MIN_TEMPERATURE, message = "Минимальная температура хранения не может быть ниже " + MIN_TEMPERATURE + "°C")
    @Max(value = MAX_TEMPERATURE, message = "Минимальная температура хранения не может быть выше " + MAX_TEMPERATURE + "°C")
    private Integer storageTemperatureMin;

    @Column(name = "storage_temperature_max", nullable = false)
    @NotNull(message = "Максимальная температура хранения обязательна")
    @Min(value = MIN_TEMPERATURE, message = "Максимальная температура хранения не может быть ниже " + MIN_TEMPERATURE + "°C")
    @Max(value = MAX_TEMPERATURE, message = "Максимальная температура хранения не может быть выше " + MAX_TEMPERATURE + "°C")
    private Integer storageTemperatureMax;

    @Column(name = "is_weighted", nullable = false)
    @NotNull(message = "Признак весового товара обязателен")
    private Boolean isWeighted;

    @Column(name = "country_of_origin", nullable = false, length = 100)
    @NotBlank(message = "Страна происхождения обязательна")
    @Size(max = 100, message = "Страна происхождения не может превышать 100 символов")
    private String countryOfOrigin;

    @Column(name = "composition", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Состав обязателен")
    @Size(max = 2000, message = "Состав не может превышать 2000 символов")
    private String composition;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Описание обязательно")
    @Size(max = 5000, message = "Описание не может превышать 5000 символов")
    private String description;

    @Column(name = "shelf_life", nullable = false, length = 100)
    @NotBlank(message = "Срок хранения обязателен")
    @Size(max = 100, message = "Срок хранения не может превышать 100 символов")
    private String shelfLife;

    @Column(nullable = false)
    @Min(0)
    @Max(100)
    @NotNull(message = "Размер скидки в процентах")
    private Integer sale = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}