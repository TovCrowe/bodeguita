package com.family.Bodeguita.user.exception;

import com.family.Bodeguita.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long id) {
        super("El usuario " + id + " no existe");
    }

    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("No hay ningún usuario con el email " + email);
    }
}
