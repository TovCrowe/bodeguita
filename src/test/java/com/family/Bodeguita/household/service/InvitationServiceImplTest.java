package com.family.Bodeguita.household.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.domain.InvitationStatus;
import com.family.Bodeguita.household.exception.AlreadyMemberException;
import com.family.Bodeguita.household.exception.EmailAlreadyInvitedException;
import com.family.Bodeguita.household.exception.EmailBelongsToAnotherHouseholdException;
import com.family.Bodeguita.household.exception.InvitationExpiredException;
import com.family.Bodeguita.household.exception.InvitationNotPendingException;
import com.family.Bodeguita.household.exception.NotInvitedException;
import com.family.Bodeguita.household.repository.InvitationRepository;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-08-03T10:00:00Z");
    private static final Duration TTL = Duration.ofDays(7);
    private static final Long LOS_TORRES = 1L;
    private static final Long OTRA_FAMILIA = 2L;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserService userService;

    private InvitationServiceImpl invitationService;
    private Household losTorres;
    private Household otraFamilia;
    private User owner;

    @BeforeEach
    void setUp() {
        invitationService =
                new InvitationServiceImpl(
                        invitationRepository,
                        userService,
                        Clock.fixed(AHORA, ZoneOffset.UTC),
                        TTL);
        losTorres = household(LOS_TORRES, "Los Torres");
        otraFamilia = household(OTRA_FAMILIA, "Otra familia");
        owner = User.owner(losTorres, "google-owner", "ana@example.com", "Ana", null);
        owner.setId(10L);
    }

    @Test
    @DisplayName("create emite una invitación PENDING con la caducidad del ttl")
    void createIssuesPendingInvitation() {
        noUserWith("beto@example.com");
        noPendingInvitationFor("beto@example.com");
        savesWhateverItGets();

        Invitation invitation = invitationService.create(losTorres, owner, "  Beto@Example.COM ");

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getEmail()).isEqualTo("beto@example.com");
        assertThat(invitation.getEmailCanonical()).isEqualTo("beto@example.com");
        assertThat(invitation.getExpiresAt()).isEqualTo(AHORA.plus(TTL));
        assertThat(invitation.getHousehold()).isSameAs(losTorres);
        assertThat(invitation.getInvitedBy()).isSameAs(owner);
    }

    @Test
    @DisplayName("create guarda la forma canónica: los puntos de Gmail no rompen el enlace")
    void createStoresTheCanonicalForm() {
        noUserWith("juan.perez@gmail.com");
        noPendingInvitationFor("juanperez@gmail.com");
        savesWhateverItGets();

        Invitation invitation = invitationService.create(losTorres, owner, "Juan.Perez@Gmail.com");

        assertThat(invitation.getEmail()).isEqualTo("juan.perez@gmail.com");
        assertThat(invitation.getEmailCanonical()).isEqualTo("juanperez@gmail.com");
    }

    @Test
    @DisplayName("reinvitar el mismo email en la misma familia renueva en vez de duplicar")
    void createRenewsInsteadOfDuplicating() {
        Invitation vigente = pendingInvitation(losTorres, "beto@example.com", AHORA.plusSeconds(60));
        noUserWith("beto@example.com");
        pendingInvitationFor("beto@example.com", vigente);
        savesWhateverItGets();

        Invitation invitation = invitationService.create(losTorres, owner, "beto@example.com");

        assertThat(invitation).isSameAs(vigente);
        assertThat(invitation.getExpiresAt()).isEqualTo(AHORA.plus(TTL));
        // Se guarda la que ya existía: no aparece una segunda fila para el mismo email.
        verify(invitationRepository).save(vigente);
        verify(invitationRepository, times(1)).save(any(Invitation.class));
    }

    @Test
    @DisplayName("una invitación vigente en otra familia bloquea, sin decir cuál")
    void createRejectsEmailInvitedElsewhere() {
        Invitation ajena = pendingInvitation(otraFamilia, "beto@example.com", AHORA.plusSeconds(60));
        noUserWith("beto@example.com");
        pendingInvitationFor("beto@example.com", ajena);

        assertThatThrownBy(() -> invitationService.create(losTorres, owner, "beto@example.com"))
                .isInstanceOf(EmailAlreadyInvitedException.class)
                .hasMessageNotContainingAny("Otra familia", String.valueOf(OTRA_FAMILIA));
    }

    @Test
    @DisplayName("una invitación caducada se marca EXPIRED y deja emitir una nueva")
    void createReplacesAnExpiredInvitation() {
        Invitation caducada =
                pendingInvitation(losTorres, "beto@example.com", AHORA.minusSeconds(1));
        noUserWith("beto@example.com");
        pendingInvitationFor("beto@example.com", caducada);
        savesWhateverItGets();

        Invitation invitation = invitationService.create(losTorres, owner, "beto@example.com");

        assertThat(caducada.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(caducada.getResolvedAt()).isEqualTo(AHORA);
        assertThat(invitation).isNotSameAs(caducada);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getExpiresAt()).isEqualTo(AHORA.plus(TTL));
    }

    @Test
    @DisplayName("no se invita a quien ya es miembro de esta familia")
    void createRejectsExistingMember() {
        User miembro = User.member(losTorres, "google-beto", "beto@example.com", "Beto", null);
        miembro.setId(11L);
        when(userService.findByEmail("beto@example.com")).thenReturn(Optional.of(miembro));

        assertThatThrownBy(() -> invitationService.create(losTorres, owner, "beto@example.com"))
                .isInstanceOf(AlreadyMemberException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se invita a quien ya pertenece a otra familia, y no se dice a cuál")
    void createRejectsMemberOfAnotherHousehold() {
        User ajeno = User.member(otraFamilia, "google-carla", "carla@example.com", "Carla", null);
        ajeno.setId(30L);
        when(userService.findByEmail("carla@example.com")).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> invitationService.create(losTorres, owner, "carla@example.com"))
                .isInstanceOf(EmailBelongsToAnotherHouseholdException.class)
                .hasMessageNotContainingAny("Otra familia", String.valueOf(OTRA_FAMILIA));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("claim consume la invitación vigente y la deja ACCEPTED")
    void claimAcceptsTheInvitation() {
        Invitation vigente = pendingInvitation(losTorres, "beto@example.com", AHORA.plusSeconds(60));
        pendingInvitationFor("beto@example.com", vigente);
        savesWhateverItGets();

        Invitation claimed = invitationService.claim("BETO@example.com");

        assertThat(claimed.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(claimed.getResolvedAt()).isEqualTo(AHORA);
        assertThat(claimed.getHousehold()).isSameAs(losTorres);
    }

    @Test
    @DisplayName("claim con alias de Gmail encuentra la invitación escrita con puntos")
    void claimMatchesGmailAliases() {
        Invitation vigente =
                pendingInvitation(losTorres, "juan.perez@gmail.com", AHORA.plusSeconds(60));
        pendingInvitationFor("juanperez@gmail.com", vigente);
        savesWhateverItGets();

        assertThat(invitationService.claim("juanperez@gmail.com").getStatus())
                .isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("claim sin invitación: nadie entra por su cuenta")
    void claimWithoutInvitation() {
        noPendingInvitationFor("intruso@example.com");

        assertThatThrownBy(() -> invitationService.claim("intruso@example.com"))
                .isInstanceOf(NotInvitedException.class);
    }

    @Test
    @DisplayName("claim de una caducada la marca EXPIRED y falla")
    void claimExpiredInvitation() {
        Invitation caducada =
                pendingInvitation(losTorres, "beto@example.com", AHORA.minusSeconds(1));
        pendingInvitationFor("beto@example.com", caducada);
        savesWhateverItGets();

        assertThatThrownBy(() -> invitationService.claim("beto@example.com"))
                .isInstanceOf(InvitationExpiredException.class);

        assertThat(caducada.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    @DisplayName("revoke deja la invitación REVOKED")
    void revokePendingInvitation() {
        Invitation vigente = pendingInvitation(losTorres, "beto@example.com", AHORA.plusSeconds(60));
        savesWhateverItGets();

        invitationService.revoke(vigente);

        assertThat(vigente.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(vigente.getResolvedAt()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("revoke de una que ya no está pendiente no hace nada")
    void revokeAlreadyResolvedInvitation() {
        Invitation aceptada = pendingInvitation(losTorres, "beto@example.com", AHORA.plusSeconds(60));
        aceptada.accept(AHORA);

        assertThatThrownBy(() -> invitationService.revoke(aceptada))
                .isInstanceOf(InvitationNotPendingException.class);

        verify(invitationRepository, never()).save(any());
    }

    private void noUserWith(String email) {
        when(userService.findByEmail(email)).thenReturn(Optional.empty());
    }

    private void noPendingInvitationFor(String canonical) {
        when(invitationRepository.findByEmailCanonicalAndStatus(canonical, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
    }

    private void pendingInvitationFor(String canonical, Invitation invitation) {
        when(invitationRepository.findByEmailCanonicalAndStatus(canonical, InvitationStatus.PENDING))
                .thenReturn(Optional.of(invitation));
    }

    private void savesWhateverItGets() {
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private Invitation pendingInvitation(Household household, String email, Instant expiresAt) {
        Invitation invitation = Invitation.pending(household, owner, email, expiresAt);
        invitation.setId(100L);
        return invitation;
    }

    private static Household household(Long id, String name) {
        Household household = new Household(name);
        household.setId(id);
        return household;
    }
}
