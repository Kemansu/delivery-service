package ru.don_polesie.back_end.model.order;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Getter
@Entity
@Table(name = "delivery_order")
@Builder
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Setter
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Указано имя колонки
    private User user;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("product ASC")
    private Set<OrderProduct> orderProducts = new HashSet<>();

    @Setter
    @Column(unique = true)
    private String paymentId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id") // Указано имя колонки
    private User employee;

    @Setter
    @Column(nullable = false)
    private Boolean needCallForApproval = false;

    public Order() {}

    public void addProduct(OrderProduct op) {
        orderProducts.add(op);
        op.setOrder(this);
    }

    public void removeProduct(OrderProduct op) {
        orderProducts.remove(op);
        op.setOrder(null);
    }
}
