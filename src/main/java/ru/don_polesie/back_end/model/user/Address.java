package ru.don_polesie.back_end.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    // Константы для валидации
    private static final int CITY_MIN_LENGTH = 2;
    private static final int CITY_MAX_LENGTH = 50;
    private static final int STREET_MIN_LENGTH = 2;
    private static final int STREET_MAX_LENGTH = 100;
    private static final int HOUSE_NUMBER_MIN = 1;
    private static final int HOUSE_NUMBER_MAX = 9999;
    private static final int APARTMENT_MIN = 1;
    private static final int APARTMENT_MAX = 9999;
    private static final int FLOOR_MIN = -10; // Подвальные этажи
    private static final int FLOOR_MAX = 500;

    // Регулярные выражения для валидации
    private static final String CITY_REGEX = "^[a-zA-Zа-яА-ЯёЁ\\s\\-'.]+$";
    private static final String STREET_REGEX = "^[a-zA-Zа-яА-ЯёЁ0-9\\s\\-.'(),]+$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", nullable = false, length = CITY_MAX_LENGTH)
    @NotBlank(message = "Город обязателен для заполнения")
    @Size(min = CITY_MIN_LENGTH, max = CITY_MAX_LENGTH,
            message = "Название города должно содержать от {min} до {max} символов")
    @Pattern(regexp = CITY_REGEX,
            message = "Название города может содержать только буквы, пробелы и символы - . '")
    private String city;

    @Column(name = "street", nullable = false, length = STREET_MAX_LENGTH)
    @NotBlank(message = "Улица обязательна для заполнения")
    @Size(min = STREET_MIN_LENGTH, max = STREET_MAX_LENGTH,
            message = "Название улицы должно содержать от {min} до {max} символов")
    @Pattern(regexp = STREET_REGEX,
            message = "Название улицы может содержать только буквы, цифры, пробелы и символы - . ' ( )")
    private String street;

    @Column(name = "house_number", nullable = false)
    @NotNull(message = "Номер дома обязателен")
    // @Min(value = HOUSE_NUMBER_MIN, message = "Номер дома должен быть не менее {value}")
    // @Max(value = HOUSE_NUMBER_MAX, message = "Номер дома не может превышать {value}")
    private String houseNumber;

    @Column(name = "apartment_number")
    @Min(value = APARTMENT_MIN, message = "Номер квартиры должен быть не менее {value}")
    @Max(value = APARTMENT_MAX, message = "Номер квартиры не может превышать {value}")
    private Integer  apartmentNumber;

    @Column(name = "floor")
    @Min(value = FLOOR_MIN, message = "Этаж не может быть меньше {value}")
    @Max(value = FLOOR_MAX, message = "Этаж не может быть больше {value}")
    private Integer  floor;

    @Column(name = "comment")
    private String comment;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean active = true;
}