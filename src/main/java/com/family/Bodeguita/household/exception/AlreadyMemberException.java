package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/** Invitar a alguien que ya está dentro. Mensaje explícito: es un error inofensivo del OWNER. */
public class AlreadyMemberException extends ConflictException {

    public AlreadyMemberException(String email) {
        super("El email " + email + " ya es miembro de esta familia");
    }
}
