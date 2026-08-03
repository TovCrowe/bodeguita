package com.family.Bodeguita.common.exception;

/**
 * Raíz de todos los errores de negocio. Cada feature define sus excepciones en su propio
 * paquete {@code exception/} heredando de una de las subclases de esta
 * ({@link NotFoundException}, {@link ForbiddenException}, {@link ConflictException}), de
 * modo que el handler global mapea esas pocas bases en lugar de una lista que crecería
 * con cada feature.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
