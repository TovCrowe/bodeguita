package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.ConflictException;

/**
 * En el MVP una persona pertenece a una sola familia. Se distingue de
 * {@link AlreadyMemberException} para que el OWNER entienda por qué falla, pero sin decirle
 * de qué otra familia se trata.
 */
public class EmailBelongsToAnotherHouseholdException extends ConflictException {

    public EmailBelongsToAnotherHouseholdException(String email) {
        super("El email " + email + " ya pertenece a otra familia");
    }
}
