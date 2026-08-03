package com.family.Bodeguita.household.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.exception.CannotRemoveSelfException;
import com.family.Bodeguita.household.exception.HouseholdNotFoundException;
import com.family.Bodeguita.household.exception.InvitationNotFoundException;
import com.family.Bodeguita.household.exception.NotHouseholdMemberException;
import com.family.Bodeguita.household.exception.NotHouseholdOwnerException;
import com.family.Bodeguita.household.repository.HouseholdRepository;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** El aislamiento por household se valida aquí, así que estos son los tests que importan. */
@ExtendWith(MockitoExtension.class)
class HouseholdServiceImplTest {

    private static final Long LOS_TORRES = 1L;
    private static final Long OTRA_FAMILIA = 2L;
    private static final Instant MANANA = Instant.parse("2026-08-04T10:00:00Z");

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private UserService userService;

    @Mock
    private InvitationService invitationService;

    private HouseholdServiceImpl householdService;
    private Household losTorres;
    private Household otraFamilia;

    @BeforeEach
    void setUp() {
        householdService =
                new HouseholdServiceImpl(householdRepository, userService, invitationService);
        losTorres = household(LOS_TORRES, "Los Torres");
        otraFamilia = household(OTRA_FAMILIA, "Otra familia");
    }

    @Test
    @DisplayName("createWithOwner crea la familia y a su OWNER")
    void createWithOwner() {
        when(householdRepository.save(any(Household.class))).thenAnswer(call -> {
            Household saved = call.getArgument(0);
            saved.setId(LOS_TORRES);
            return saved;
        });

        Household created = householdService.createWithOwner(
                "Los Torres", "google-123", "ana@example.com", "Ana", "http://avatar");

        assertThat(created.getName()).isEqualTo("Los Torres");
        verify(userService)
                .createOwner(created, "google-123", "ana@example.com", "Ana", "http://avatar");
    }

    @Test
    @DisplayName("inviteMember: el OWNER invita en su familia y queda registrado quién invitó")
    void inviteMemberAsOwner() {
        User owner = owner(10L, losTorres);
        Invitation emitida = invitation(losTorres, owner, "nuevo@example.com");
        when(userService.getById(10L)).thenReturn(owner);
        when(householdRepository.findById(LOS_TORRES)).thenReturn(Optional.of(losTorres));
        when(invitationService.create(losTorres, owner, "nuevo@example.com")).thenReturn(emitida);

        Invitation created = householdService.inviteMember(LOS_TORRES, 10L, "nuevo@example.com");

        assertThat(created).isSameAs(emitida);
        verify(invitationService).create(losTorres, owner, "nuevo@example.com");
    }

    @Test
    @DisplayName("inviteMember: un MEMBER no puede invitar")
    void inviteMemberAsPlainMember() {
        when(userService.getById(11L)).thenReturn(member(11L, losTorres));

        assertThatThrownBy(() -> householdService.inviteMember(LOS_TORRES, 11L, "nuevo@example.com"))
                .isInstanceOf(NotHouseholdOwnerException.class);

        verify(invitationService, never()).create(any(), any(), anyString());
    }

    @Test
    @DisplayName("inviteMember: un OWNER de otra familia no puede tocar esta")
    void inviteMemberFromAnotherHousehold() {
        when(userService.getById(20L)).thenReturn(owner(20L, otraFamilia));

        assertThatThrownBy(() -> householdService.inviteMember(LOS_TORRES, 20L, "nuevo@example.com"))
                .isInstanceOf(NotHouseholdMemberException.class);

        verify(invitationService, never()).create(any(), any(), anyString());
    }

    @Test
    @DisplayName("revokeInvitation: el OWNER revoca una invitación de su familia")
    void revokeInvitationAsOwner() {
        User owner = owner(10L, losTorres);
        Invitation emitida = invitation(losTorres, owner, "nuevo@example.com");
        when(userService.getById(10L)).thenReturn(owner);
        when(invitationService.getById(100L)).thenReturn(emitida);

        householdService.revokeInvitation(LOS_TORRES, 10L, 100L);

        verify(invitationService).revoke(emitida);
    }

    @Test
    @DisplayName("revokeInvitation: una invitación de otra familia se ve como inexistente")
    void revokeInvitationFromAnotherHousehold() {
        User owner = owner(10L, losTorres);
        when(userService.getById(10L)).thenReturn(owner);
        when(invitationService.getById(100L))
                .thenReturn(invitation(otraFamilia, owner, "nuevo@example.com"));

        assertThatThrownBy(() -> householdService.revokeInvitation(LOS_TORRES, 10L, 100L))
                .isInstanceOf(InvitationNotFoundException.class);

        verify(invitationService, never()).revoke(any());
    }

    @Test
    @DisplayName("pendingInvitations: solo el OWNER ve las invitaciones pendientes")
    void pendingInvitationsAsPlainMember() {
        when(userService.getById(11L)).thenReturn(member(11L, losTorres));

        assertThatThrownBy(() -> householdService.pendingInvitations(LOS_TORRES, 11L))
                .isInstanceOf(NotHouseholdOwnerException.class);

        verify(invitationService, never()).pendingFor(any());
    }

    @Test
    @DisplayName("acceptInvitation da de alta al invitado en la familia que lo invitó")
    void acceptInvitation() {
        User owner = owner(10L, losTorres);
        Invitation emitida = invitation(losTorres, owner, "beto@example.com");
        User nuevo = member(11L, losTorres);
        when(invitationService.claim("beto@example.com")).thenReturn(emitida);
        when(userService.createMember(losTorres, "google-beto", "beto@example.com", "Beto", null))
                .thenReturn(nuevo);

        User created =
                householdService.acceptInvitation("google-beto", "beto@example.com", "Beto", null);

        assertThat(created).isSameAs(nuevo);
        verify(invitationService).claim("beto@example.com");
    }

    @Test
    @DisplayName("removeMember: el OWNER quita a un miembro de su familia")
    void removeMemberAsOwner() {
        User owner = owner(10L, losTorres);
        User victima = member(11L, losTorres);
        when(userService.getById(10L)).thenReturn(owner);
        when(userService.getById(11L)).thenReturn(victima);

        householdService.removeMember(LOS_TORRES, 10L, 11L);

        verify(userService).delete(victima);
    }

    @Test
    @DisplayName("removeMember: el OWNER no puede quitarse a sí mismo")
    void removeMemberSelf() {
        when(userService.getById(10L)).thenReturn(owner(10L, losTorres));

        assertThatThrownBy(() -> householdService.removeMember(LOS_TORRES, 10L, 10L))
                .isInstanceOf(CannotRemoveSelfException.class);

        verify(userService, never()).delete(any());
    }

    @Test
    @DisplayName("removeMember: no se puede quitar a alguien de otra familia")
    void removeMemberFromAnotherHousehold() {
        when(userService.getById(10L)).thenReturn(owner(10L, losTorres));
        when(userService.getById(30L)).thenReturn(member(30L, otraFamilia));

        assertThatThrownBy(() -> householdService.removeMember(LOS_TORRES, 10L, 30L))
                .isInstanceOf(NotHouseholdMemberException.class);

        verify(userService, never()).delete(any());
    }

    @Test
    @DisplayName("members: cualquier miembro lista a los suyos")
    void membersAsMember() {
        User miembro = member(11L, losTorres);
        when(userService.getById(11L)).thenReturn(miembro);
        when(userService.findAllByHousehold(LOS_TORRES)).thenReturn(List.of(miembro));

        assertThat(householdService.members(LOS_TORRES, 11L)).containsExactly(miembro);
    }

    @Test
    @DisplayName("members: nadie lista a los miembros de otra familia")
    void membersFromAnotherHousehold() {
        when(userService.getById(30L)).thenReturn(member(30L, otraFamilia));

        assertThatThrownBy(() -> householdService.members(LOS_TORRES, 30L))
                .isInstanceOf(NotHouseholdMemberException.class);

        verify(userService, never()).findAllByHousehold(any());
    }

    @Test
    @DisplayName("getById lanza HouseholdNotFoundException si la familia no existe")
    void getByIdMissing() {
        when(householdRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> householdService.getById(99L))
                .isInstanceOf(HouseholdNotFoundException.class);
    }

    private static Household household(Long id, String name) {
        Household household = new Household(name);
        household.setId(id);
        return household;
    }

    private static User owner(Long id, Household household) {
        User user = User.owner(
                household, "google-" + id, "owner" + id + "@example.com", "Owner " + id, null);
        user.setId(id);
        return user;
    }

    private static User member(Long id, Household household) {
        User user = User.member(
                household, "google-" + id, "member" + id + "@example.com", "Member " + id, null);
        user.setId(id);
        return user;
    }

    private static Invitation invitation(Household household, User invitedBy, String email) {
        Invitation invitation = Invitation.pending(household, invitedBy, email, MANANA);
        invitation.setId(100L);
        return invitation;
    }
}
