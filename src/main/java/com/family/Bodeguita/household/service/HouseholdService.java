package com.family.Bodeguita.household.service;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.user.domain.User;
import java.util.List;

/**
 * Dueño de las membresías: aquí vive la regla de que un usuario solo opera sobre su
 * propia familia y de que solo el OWNER invita o quita miembros.
 *
 * <p>Cada operación recibe el {@code requesterId} porque el aislamiento por household se
 * valida en esta capa, no en el controller. La excepción es
 * {@link #acceptInvitation}, que ocurre durante el login: quien llama todavía no es usuario.
 */
public interface HouseholdService {

    Household getById(Long householdId);

    /** Primer login sin invitación: el usuario crea su familia y queda como OWNER. */
    Household createWithOwner(
            String name, String googleSub, String email, String displayName, String avatarUrl);

    /** El OWNER invita por email. Reinvitar el mismo email renueva la invitación vigente. */
    Invitation inviteMember(Long householdId, Long requesterId, String email);

    void revokeInvitation(Long householdId, Long requesterId, Long invitationId);

    List<Invitation> pendingInvitations(Long householdId, Long requesterId);

    /**
     * Primer login de un invitado: consume la invitación vigente de ese email y lo da de alta
     * como MEMBER de esa familia.
     *
     * <p>El {@code email} debe venir del ID token de Google y solo si {@code email_verified}
     * es cierto: es lo único que prueba que quien entra es el dueño del buzón invitado.
     */
    User acceptInvitation(String googleSub, String email, String name, String avatarUrl);

    void removeMember(Long householdId, Long requesterId, Long memberId);

    List<User> members(Long householdId, Long requesterId);
}
