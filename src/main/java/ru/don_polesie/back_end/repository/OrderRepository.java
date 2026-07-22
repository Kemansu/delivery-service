package ru.don_polesie.back_end.repository;

import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.model.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @NonNull
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderProducts WHERE o.id = :id")
    Optional<Order> findById(@Param("id") @NonNull Long id);

    // Блокировка строки заказа (SELECT ... FOR UPDATE) на время транзакции —
    // сериализует одновременные takeOrder/processOrder/markShipped/cancel по
    // одному заказу, чтобы проверка статуса и запись были атомарны (без гонки
    // «двое забрали один заказ»).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    void deleteById(@NonNull Long integer);

    // Количество продаж за день выбранного товара
    @Query("SELECT COALESCE(SUM(CAST(op.quantity AS long)), 0) " +
            "FROM OrderProduct op " +
            "WHERE op.product.id = :productId " +
            "AND FUNCTION('DATE', op.order.createdAt) = FUNCTION('DATE', :date)")
    Long getTotalSalesForProductByDate(@Param("productId") Long productId, @Param("date") Instant date);

    // Количество заказов за день, содержащих выбранный товар
    @Query("SELECT COUNT(DISTINCT op.order.id) " +
            "FROM OrderProduct op " +
            "WHERE op.product.id = :productId " +
            "AND FUNCTION('DATE', op.order.createdAt) = FUNCTION('DATE', :date)")
    Long getOrderCountForProductByDate(@Param("productId") Long productId, @Param("date") Instant date);

    Page<Order> findByUserPhoneNumber(String phoneNumber, Pageable pageable);

    Page<Order> findByUserPhoneNumberAndStatus(String phoneNumber, OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndPaymentIdIsNotNull(OrderStatus status);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    Optional<Order> findOrderByStatusAndEmployee(OrderStatus status, User employee);

    // Можно обернуть в удобный метод для "PAID"
    default Page<Order> findAllPaid(Pageable pageable) {
        return findAllByStatus(OrderStatus.PAID, pageable);
    }

    default Page<Order> findAllMoneyReservaited(Pageable pageable) {
        return findAllByStatus(OrderStatus.MONEY_RESERVAITED, pageable);
    }

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end AND o.status = ru.don_polesie.back_end.model.enums.OrderStatus.SHIPPED")
    BigDecimal calculateRevenueByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
