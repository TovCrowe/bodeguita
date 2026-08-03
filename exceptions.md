# Manejo de excepciones

Léelo cuando lances errores desde un service o toques el handler global.

Modelo en dos niveles:

1. **Excepciones de negocio por feature.** Cada feature define las suyas en su
   `exception/` (p. ej. `pantry/exception/PantryItemNotFoundException`,
   `household/exception/NotHouseholdMemberException`). Las **lanza el service**, con
   mensaje claro. No usar excepciones genéricas de Java para reglas de negocio.
2. **Traducción a HTTP centralizada.** En `common/` vive un único
   **`GlobalExceptionHandler` (`@RestControllerAdvice`)** que las convierte en respuesta
   HTTP. Es el **único** lugar que arma respuestas de error; los controllers nunca hacen
   `try/catch` para formatear errores.

Formato consistente (record `ApiError` en `common/`):

```json
{ "timestamp": "...", "status": 404, "error": "Not Found",
  "message": "PantryItem 42 no existe", "path": "/pantry/42" }
```

Mapeo excepción → status:

| Excepción                                               | HTTP |
|---------------------------------------------------------|------|
| `*NotFoundException`                                    | 404  |
| `NotHouseholdMemberException` / acceso a otra familia   | 403  |
| `MethodArgumentNotValidException` (validación `@Valid`) | 400  |
| conflicto de estado (ej. email ya invitado)             | 409  |
| no capturada / inesperada                               | 500  |

Recomendación: una base común `DomainException` (en `common/`) de la que hereden las
excepciones de negocio, para simplificar el handler.
