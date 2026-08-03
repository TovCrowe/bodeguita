package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/**
 * Ese email ya tiene una invitación vigente en otra familia. No se dice en cuál: el OWNER
 * no tiene por qué enterarse de a quién más invitaron.
 */
public class EmailAlreadyInvitedException extends ConflictException {

    public EmailAlreadyInvitedException(String email) {
        super("El email " + email + " ya tiene una invitación pendiente");
    }
}
