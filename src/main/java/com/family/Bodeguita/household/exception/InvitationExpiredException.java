package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/** Login con una invitación vencida. El OWNER tiene que volver a invitar. */
public class InvitationExpiredException extends ConflictException {

    public InvitationExpiredException(String email) {
        super("La invitación de " + email + " caducó; pide que te vuelvan a invitar");
    }
}
