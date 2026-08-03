package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ForbiddenException;

/** Solo el OWNER puede agregar o quitar miembros. */
public class NotHouseholdOwnerException extends ForbiddenException {

    public NotHouseholdOwnerException(Long userId, Long householdId) {
        super("El usuario " + userId + " no es OWNER de la familia " + householdId);
    }
}
