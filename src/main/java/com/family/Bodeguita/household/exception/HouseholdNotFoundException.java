package com.family.Bodeguita.household.exception;

import com.family.Bodeguita.common.exception.NotFoundException;

public class HouseholdNotFoundException extends NotFoundException {

    public HouseholdNotFoundException(Long id) {
        super("La familia " + id + " no existe");
    }
}
