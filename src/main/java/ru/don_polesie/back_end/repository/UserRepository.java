package ru.don_polesie.back_end.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.don_polesie.back_end.model.user.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String number);

    Page<User> findByRolesName(String roleName, Pageable pageable);

    boolean existsByPhoneNumberAndActiveTrue(String phoneNumber);

    Page<User> findDistinctByRolesNameIn(Collection<String> roleNames, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByLoginIgnoreCase(String login);
}
