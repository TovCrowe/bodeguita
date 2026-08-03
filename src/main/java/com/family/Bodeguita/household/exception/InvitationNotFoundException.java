package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.NotFoundException;

/** También se lanza si la invitación existe pero es de otra familia: no se filtra su existencia. */
public class InvitationNotFoundException extends NotFoundException {

    public InvitationNotFoundException(Long id) {
        super("La invitación " + id + " no existe");
    }
}
