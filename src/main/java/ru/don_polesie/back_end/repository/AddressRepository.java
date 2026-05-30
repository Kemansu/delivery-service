package ru.don_polesie.back_end.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserAndActiveTrue(User user);

    @Modifying
    @Query("UPDATE Address a SET a.active = false WHERE a.id = :id AND a.user = :user")
    void deactivateById(@Param("id") Long id, @Param("user") User user);

    boolean existsByIdAndUser(Long id, User user);

    @Modifying
    @Query("UPDATE Address a SET a.comment = :comment WHERE a.id = :id AND a.user = :user")
    void updateComment(@Param("id") Long id, @Param("comment") String comment, @Param("user") User user);
}
