package com.family.Bodeguita.common.exception;

/** El recurso pedido no existe. Se traduce a 404. */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String message) {
        super(message);
    }
}
