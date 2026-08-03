# Configuración, base de datos y perfiles

Léelo cuando toques `application*.properties`, migraciones o variables de entorno.

## Perfiles

- `dev` (default): H2 en memoria (`MODE=PostgreSQL`), Flyway apagado,
  `ddl-auto=create-drop`, scraping apagado, logging verboso, consola H2 en `/h2-console`.
- `prod`: PostgreSQL, Flyway encendido, `ddl-auto=validate`, scraping activo.
- `test`: H2, contexto mínimo.

Config común en `application.properties`; específicos en `application-dev.properties` y
`application-prod.properties`. El perfil se elige con `SPRING_PROFILES_ACTIVE`.

## Base de datos y migraciones

- Todo cambio de esquema es una migración **Flyway** en `src/main/resources/db/migration`
  (`V1__init.sql`, `V2__add_price_snapshot.sql`, ...). No usar `ddl-auto=update` en prod.
- El esquema autoritativo son las migraciones de Flyway; en prod Hibernate solo
  **valida** (`ddl-auto=validate`).
- En dev, Flyway va **apagado** y Hibernate genera el esquema (`create-drop`) para
  iterar rápido sin pelear con SQL específico de Postgres sobre H2.
- Índices: `user.googleSub` único, `pantry_item(household_id)`, `product.barcode`.

## Variables de entorno

Definir en el entorno (nunca commitear secretos; añadir `.env` al `.gitignore`):

```
SPRING_PROFILES_ACTIVE=dev|prod
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
JWT_SECRET=...                  # o par de llaves si se usa RS256
DATABASE_URL=jdbc:postgresql://localhost:5432/Bodeguita
DATABASE_USER=...
DATABASE_PASSWORD=...
CORS_ALLOWED_ORIGINS=...        # requerido en prod (sin default: la app truena si falta)
```

Propiedades custom que necesitan su clase `@ConfigurationProperties`: `app.jwt.*`,
`app.scraping.*`, `app.cors.*`.
