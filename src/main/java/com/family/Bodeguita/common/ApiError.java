package com.family.Bodeguita.common;

import java.time.Instant;

/** Cuerpo único de todas las respuestas de error de la API. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
