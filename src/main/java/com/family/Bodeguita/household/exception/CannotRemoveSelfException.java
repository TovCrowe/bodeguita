package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/**
 * Un OWNER no puede quitarse a sí mismo: dejaría a la familia potencialmente sin dueño.
 * Salir de la familia por voluntad propia sería otra operación, con sus propias reglas.
 */
public class CannotRemoveSelfException extends ConflictException {

    public CannotRemoveSelfException(Long userId) {
        super("El usuario " + userId + " no puede quitarse a sí mismo de la familia");
    }
}
