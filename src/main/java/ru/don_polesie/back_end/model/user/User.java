package ru.don_polesie.back_end.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.don_polesie.back_end.model.Role;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.model.order.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "delivery_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone_number"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "roles", "addresses", "orders", "basket"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    // Константы для валидации
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int SURNAME_MIN_LENGTH = 2;
    private static final int SURNAME_MAX_LENGTH = 50;
    private static final int PHONE_LENGTH = 11;
    private static final String PHONE_REGEX = "^7[0-9]{10}$";
    private static final int EMAIL_MAX_LENGTH = 100;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final int PASSWORD_MIN_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", length = NAME_MAX_LENGTH)
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH,
            message = "Имя должно содержать от {min} до {max} символов")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ\\s-]+$",
            message = "Имя может содержать только буквы, пробелы и дефисы")
    private String name;

    @Column(name = "surname", length = SURNAME_MAX_LENGTH)
    @Size(min = SURNAME_MIN_LENGTH, max = SURNAME_MAX_LENGTH,
            message = "Фамилия должна содержать от {min} до {max} символов")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ\\s-]+$",
            message = "Фамилия может содержать только буквы, пробелы и дефисы")
    private String surname;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "Пароль обязателен")
    @Size(min = PASSWORD_MIN_LENGTH, message = "Пароль должен содержать не менее {min} символов")
    @JsonIgnore
    private String password;

    @Column(name = "phone_number", unique = true, nullable = false, length = PHONE_LENGTH)
    @NotBlank(message = "Номер телефона обязателен")
    @Size(min = PHONE_LENGTH, max = PHONE_LENGTH, message = "Номер телефона должен содержать {min} цифр")
    @Pattern(regexp = PHONE_REGEX, message = "Номер телефона должен быть в формате 7XXXXXXXXXX")
    private String phoneNumber;

    @Column(name = "email", unique = true, length = EMAIL_MAX_LENGTH)
    @Email(message = "Email должен быть валидным")
    @Size(max = EMAIL_MAX_LENGTH, message = "Email не может превышать {max} символов")
    private String email;

    // Логин для входа в админ-панель (сотрудники); покупателям не выдаётся.
    // Уникальность обеспечивает индекс в БД (колонка добавлена вручную 21.07.2026).
    @Column(name = "login", length = 50)
    private String login;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    )
    @Builder.Default
    @NotNull(message = "Роли пользователя не могут быть null")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Basket basket;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;
}