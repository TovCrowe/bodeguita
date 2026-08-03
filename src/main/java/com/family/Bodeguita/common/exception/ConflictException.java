package com.family.Bodeguita.common.exception;

/** La operación choca con el estado actual del recurso. Se traduce a 409. */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String message) {
        super(message);
    }
}
