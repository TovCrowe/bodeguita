package com.family.Bodeguita.household.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.domain.InvitationStatus;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

/**
 * El único parcial ({@code WHERE status = 'PENDING'}) no existe en H2, así que aquí solo se
 * comprueban las consultas; que no haya dos PENDING por email lo garantizan
 * {@code InvitationService} y, en prod, el índice de {@code V1__init.sql}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InvitationRepositoryTest {

    private static final Instant MANANA = Instant.parse("2026-08-04T10:00:00Z");

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private Household losTorres;
    private Household otraFamilia;
    private User owner;
    private User otroOwner;

    @BeforeEach
    void setUp() {
        losTorres = householdRepository.save(new Household("Los Torres"));
        otraFamilia = householdRepository.save(new Household("Otra familia"));
        owner = userRepository.save(
                User.owner(losTorres, "google-1", "ana@example.com", "Ana", null));
        otroOwner = userRepository.save(
                User.owner(otraFamilia, "google-2", "carla@example.com", "Carla", null));
    }

    @Test
    @DisplayName("findByEmailCanonicalAndStatus ignora las que ya no están pendientes")
    void findByEmailCanonicalAndStatus() {
        Invitation aceptada =
                Invitation.pending(losTorres, owner, "beto@example.com", MANANA);
        aceptada.accept(Instant.now());
        invitationRepository.save(aceptada);

        assertThat(invitationRepository.findByEmailCanonicalAndStatus(
                        "beto@example.com", InvitationStatus.PENDING))
                .isEmpty();
        assertThat(invitationRepository.findByEmailCanonicalAndStatus(
                        "beto@example.com", InvitationStatus.ACCEPTED))
                .isPresent();
    }

    @Test
    @DisplayName("findByEmailCanonicalAndStatus busca por la clave canónica, no por lo escrito")
    void findByEmailCanonicalMatchesGmailAliases() {
        invitationRepository.save(
                Invitation.pending(losTorres, owner, "Juan.Perez@Gmail.com", MANANA));

        assertThat(invitationRepository.findByEmailCanonicalAndStatus(
                        "juanperez@gmail.com", InvitationStatus.PENDING))
                .get()
                .extracting(Invitation::getEmail)
                .isEqualTo("juan.perez@gmail.com");
    }

    @Test
    @DisplayName("findAllByHouseholdIdAndStatus no devuelve invitaciones de otra familia")
    void pendingInvitationsAreIsolatedByHousehold() {
        invitationRepository.save(Invitation.pending(losTorres, owner, "beto@example.com", MANANA));
        invitationRepository.save(Invitation.pending(losTorres, owner, "dani@example.com", MANANA));
        invitationRepository.save(
                Invitation.pending(otraFamilia, otroOwner, "elsa@example.com", MANANA));

        assertThat(invitationRepository.findAllByHouseholdIdAndStatus(
                        losTorres.getId(), InvitationStatus.PENDING))
                .extracting(Invitation::getEmail)
                .containsExactlyInAnyOrder("beto@example.com", "dani@example.com");
    }
}
