package com.family.Bodeguita.common.exception;

/** El usuario está autenticado pero no tiene permiso sobre el recurso. Se traduce a 403. */
public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String message) {
        super(message);
    }
}
