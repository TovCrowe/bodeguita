# CLAUDE.md — Despensa Tracker

Índice del proyecto. Lee las **reglas de oro** siempre; el detalle está en `docs/`
(léelo solo cuando toques esa área).

## Qué es

API REST del **inventario de despensa compartido de una familia** (household). Los
miembros comparten un inventario; un ítem se enlaza a un producto del catálogo, y del
producto se obtiene un **precio base** por scraping de tiendas (Sam's, Walmart, Bodega).

MVP: login con Google · un usuario pertenece a una familia · CRUD de ítems · precio por
scraping (job diario).

## Stack

Java 21 · Spring Boot 3.x · Maven (`./mvnw`) · Spring Web (REST, sin frontend) ·
Spring Data JPA · **Postgres** (prod) / **H2** (dev-test) · Spring Security +
OAuth2 Client (Google) + JWT propio · Flyway · Jsoup (scraping) ·
JUnit 5 / MockMvc / Testcontainers.

## Comandos

```bash
./mvnw spring-boot:run                                   # dev (H2)
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod   # prod (Postgres)
./mvnw test                                              # tests
./mvnw clean package                                     # jar
docker compose up -d postgres                            # Postgres local
```

## Reglas de oro (no romper)

- **Aislamiento por household:** toda operación de inventario se valida contra el
  household del usuario autenticado, en la capa **service**. Nunca acceder a otra familia.
- **DTOs en el borde:** nunca exponer entidades JPA en los controladores; entra/sale por
  `record`. La conversión va en `mapper/`.
- **Capas:** controller → service → repository → domain. El controller no toca el
  repository directo.
- **Excepciones:** de negocio en el `exception/` de cada feature, lanzadas por el service;
  traducidas a HTTP solo por el handler global en `common/`. Sin `try/catch` de formateo.
- **Scraping fuera del request:** solo en el job `@Scheduled`, aislado tras `PriceProvider`.
- **Migraciones:** todo cambio de esquema es una migración Flyway. `ddl-auto=validate` en prod.
- **Secretos:** siempre por variables de entorno, nunca en el repo.
- Antes de terminar una tarea: `./mvnw test` en verde.
- Si una decisión pendiente bloquea el trabajo, **preguntar** en vez de asumir.

## Estructura

Package-by-feature. Cada feature: `controller / service / repository / domain / dto /
mapper / exception / util` (los que apliquen). Detalle en `docs/architecture.md`.

```
com.familia.Bodeguita
├── auth/ user/ household/ pantry/ product/ pricing/
├── common/   # handler global de excepciones, ApiError, utils compartidos
└── config/   # CORS, OpenAPI, etc.
```

## Documentación

- `docs/architecture.md` — estructura por feature, modelo de dominio, convenciones, testing.
- `docs/auth.md` — login con Google, JWT propio, membresías.
- `docs/pricing.md` — scraping de precios (job diario).
- `docs/exceptions.md` — manejo de excepciones y códigos HTTP.
- `docs/config.md` — perfiles, base de datos, migraciones y variables de entorno.

## Decisiones (estado)

- [x] Sesión: JWT propio (`docs/auth.md`).
- [x] Alta de miembros: el `OWNER` agrega manual por email (`docs/auth.md`).
- [x] Precios: job programado diario (`docs/pricing.md`).
- [ ] ¿Alertas de bajo stock / caducidad en MVP o fase 2? *(pendiente)*
