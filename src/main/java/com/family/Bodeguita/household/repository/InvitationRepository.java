package com.family.Bodeguita.household.repository;

import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.domain.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /**
     * Devuelve como mucho una: el índice único parcial garantiza una sola PENDING por email.
     * Ojo, "PENDING" no implica "vigente"; la caducidad se comprueba contra {@code expiresAt}.
     */
    Optional<Invitation> findByEmailCanonicalAndStatus(String emailCanonical, InvitationStatus status);

    List<Invitation> findAllByHouseholdIdAndStatus(Long householdId, InvitationStatus status);
}
