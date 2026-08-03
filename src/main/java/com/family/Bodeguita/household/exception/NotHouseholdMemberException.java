package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ForbiddenException;

/** Intento de tocar el inventario o las membresías de otra familia. */
public class NotHouseholdMemberException extends ForbiddenException {

    public NotHouseholdMemberException(Long userId, Long householdId) {
        super("El usuario " + userId + " no pertenece a la familia " + householdId);
    }
}
