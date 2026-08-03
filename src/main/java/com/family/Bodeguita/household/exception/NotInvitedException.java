package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ForbiddenException;

/** Login correcto con Google, pero nadie invitó a esa persona: no se le crea familia sola. */
public class NotInvitedException extends ForbiddenException {

    public NotInvitedException(String email) {
        super("No hay ninguna invitación pendiente para " + email);
    }
}
