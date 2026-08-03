package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/** Ya fue aceptada, revocada o caducada: no queda nada que revocar. */
public class InvitationNotPendingException extends ConflictException {

    public InvitationNotPendingException(Long id) {
        super("La invitación " + id + " ya no está pendiente");
    }
}
