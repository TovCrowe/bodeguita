package com.family.Bodeguita.household.service;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.user.domain.User;
import java.util.List;

/**
 * Ciclo de vida de la invitación. No decide quién puede invitar: eso lo valida
 * {@code HouseholdService} antes de llamar aquí, igual que con {@code UserService}.
 */
public interface InvitationService {

    Invitation getById(Long id);

    /**
     * Emite la invitación. Si ese email ya tenía una vigente en esta misma familia, la
     * renueva en lugar de duplicarla: reinvitar es reenviar.
     */
    Invitation create(Household household, User invitedBy, String email);

    List<Invitation> pendingFor(Long householdId);

    void revoke(Invitation invitation);

    /**
     * Reclama la invitación vigente de ese email y la deja ACCEPTED. El {@code User} lo crea
     * quien llama, con la familia de la invitación devuelta.
     */
    Invitation claim(String email);
}
