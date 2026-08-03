package com.family.Bodeguita.user.exception;

import com.family.Bodeguita.common.exception.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("El email " + email + " ya está registrado");
    }
}
