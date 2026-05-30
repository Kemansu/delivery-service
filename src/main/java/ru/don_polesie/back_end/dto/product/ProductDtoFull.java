package ru.don_polesie.back_end.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ProductDtoFull {

    private Long id;

    @NotBlank(message = "Бренд не должен быть пустым")
    @Size(max = 255, message = "Бренд должен содержать не более 255 символов")
    private String brand;

    private int brandId;

    @NotBlank(message = "Имя не должно быть пустым")
    @Size(max = 255, message = "Имя должно содержать не более 255 символов")
    private String name;

    @NotBlank(message = "Категория не должна быть пустой")
    @Size(max = 255, message = "Категория должна содержать не более 255 символов")
    private String category;

    private int categoryId;

    @NotBlank(message = "Фото не должно быть пустым")
    private String imageUrl;

    @NotNull(message = "Цена обязательна")
    @PositiveOrZero(message = "Цена не может быть меньше 0")
    private Double price;

    @NotNull(message = "Жиры обязательны")
    @DecimalMin(value = "0.0", message = "Жиры не могут быть меньше 0")
    private Double fatGrams;

    @NotNull(message = "Белки обязательны")
    @DecimalMin(value = "0.0", message = "Белки не могут быть меньше 0")
    private Double proteinGrams;

    @NotNull(message = "Углеводы обязательны")
    @DecimalMin(value = "0.0", message = "Углеводы не могут быть меньше 0")
    private Double carbohydrateGrams;

    @NotNull(message = "Энергетическая ценность обязательна")
    @Min(value = 0, message = "Энергетическая ценность не может быть отрицательной")
    private Integer energyKcalPer100g;

    @Min(value = 0, message = "Вес товара не может быть отрицательным")
    private Integer minWeight;

    @NotNull(message = "Максимальный вес товара обязателен")
    @Min(value = 0, message = "Вес товара не может быть отрицательным")
    private Integer maxWeight;

    @NotNull(message = "Количество товара обязателно")
    @Min(value = 1, message = "Количество товара должно быть положительным")
    private Integer amount;

    @NotNull(message = "Минимальная температура хранения обязательна")
    private Integer storageTemperatureMin;

    private Boolean isWeighted;

    @NotNull(message = "Состав товара обязателно")
    private String composition;

    @NotNull(message = "Описание товара обязателно")
    private String description;

    @NotNull(message = "Срок хранения товара обязателно")
    private String shelfLife;

    @NotNull(message = "Максимальная температура хранения обязательна")
    @AssertTrue(message = "Максимальная температура должна быть больше или равна минимальной")
    private boolean isValidTemperatureRange() {
        if (storageTemperatureMin == null || storageTemperatureMax == null) return true;
        return storageTemperatureMax >= storageTemperatureMin;
    }

    @NotNull(message = "Максимальная температура хранения обязательна")
    private Integer storageTemperatureMax;


    @NotBlank(message = "Страна-производитель обязательна")
    private String countryOfOrigin;

    @NotNull(message = "Размер скидки в процентах обязателен")
    private Integer sale;
}
