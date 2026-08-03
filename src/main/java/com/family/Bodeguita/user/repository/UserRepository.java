package com.family.Bodeguita.user.repository;

import com.family.Bodeguita.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSub(String googleSub);

    /** Siempre por la clave canónica: la columna {@code email} es solo para mostrar. */
    Optional<User> findByEmailCanonical(String emailCanonical);

    boolean existsByEmailCanonical(String emailCanonical);

    List<User> findAllByHouseholdId(Long householdId);
}
