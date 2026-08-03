# Arquitectura

Léelo cuando vayas a crear/mover features, entidades o DTOs.

## Estructura por feature (package-by-feature)

Top level:

```
com.familia.Bodeguita
├── auth/         # login Google, emisión/validación de token, SecurityConfig
├── user/         # User (miembro de familia), perfil
├── household/    # Household (la familia), membresías, alta manual de miembros
├── pantry/       # PantryItem — inventario, CRUD principal
├── product/      # Product — catálogo (nombre, marca, código de barras)
├── pricing/      # PriceProvider, scrapers por tienda, PriceSnapshot
├── common/       # handler global de excepciones, ApiError, base classes, utils compartidos
└── config/       # configuración transversal (CORS, OpenAPI, etc.)
```

Cada feature tiene una estructura interna **fija** (usa solo los sub-paquetes que apliquen):

```
com.familia.Bodeguita.pantry            # ejemplo con TODOS los sub-paquetes
├── controller/    # PantryController — endpoints REST, solo orquesta
├── service/       # PantryService (interfaz) + PantryServiceImpl — lógica de negocio
├── repository/    # PantryItemRepository — Spring Data JPA
├── domain/        # entidades JPA: PantryItem, enums (Unit, Location)
├── dto/           # records de entrada/salida: CreatePantryItemRequest, PantryItemResponse
├── mapper/        # PantryItemMapper — entidad ↔ DTO (MapStruct o manual)
├── exception/     # excepciones propias del feature: PantryItemNotFoundException
└── util/          # helpers específicos del feature (si hacen falta)
```

## Reglas de capas

- **controller → service → repository → domain**. El controller nunca toca el
  repository directo; el repository nunca devuelve DTOs.
- **Nunca exponer entidades JPA** en los controladores: entra y sale por **DTOs** (`record`).
- La conversión entidad ↔ DTO vive en `mapper/`, no en el controller ni el service.
- Las **excepciones de negocio** se definen en el `exception/` de su feature y se lanzan
  desde el service (ver `docs/exceptions.md`).
- `util/` solo para helpers propios del feature; lo compartido va en `common/`.

## Modelo de dominio

- **Household**: `id`, `name`, `createdAt`. Unidad de compartición del inventario.
- **User**: `id`, `googleSub` (único, nullable si `PENDING`), `email`, `name`,
  `avatarUrl`, `household` (FK), `role` (`OWNER` | `MEMBER`), `status`
  (`PENDING` | `ACTIVE`), `createdAt`.
- **Product** (catálogo): `id`, `name`, `brand`, `barcode`, `imageUrl`, `category`.
  Se reutiliza entre familias; no es propiedad de nadie.
- **PantryItem** (inventario): `id`, `household` (FK), `product` (FK), `quantity`,
  `unit` (`PZA` | `KG` | `L` | ...), `location` (`DESPENSA` | `REFRI` | `CONGELADOR`),
  `expiryDate?`, `minQuantity?`, `addedBy`, `updatedBy`, `createdAt`, `updatedAt`.
- **PriceSnapshot**: `id`, `product` (FK), `store` (`SAMS` | `WALMART` | `BODEGA`),
  `price`, `currency` (`MXN`), `sourceUrl`, `scrapedAt`. Histórico; el "precio base"
  es el snapshot más reciente por tienda.

## Convenciones de código

- **Inyección por constructor** (nada de `@Autowired` en campos).
- DTOs como `record`; validación con **Jakarta Validation** (`@Valid`, `@NotNull`...).
- Códigos HTTP: `201` al crear, `204` al borrar, `404` si no existe, `403` si no
  pertenece al household, `400` en validación.
- Nombres en el idioma del negocio (ubiquitous language): `PantryItem`, `Household`,
  `PriceSnapshot`. Consistencia en toda la base.
- No introducir dependencias nuevas sin justificarlo.

## Testing

- **TDD preferido**: el test que falla antes de la implementación.
- Unitarios: servicios con dependencias mockeadas.
- Integración: controladores con **MockMvc**; persistencia con **Testcontainers**
  (Postgres) o H2 para lo ligero.
- Cubrir siempre: aislamiento por household, CRUD de inventario, y el parseo de cada
  scraper con fixtures HTML (nunca red real en tests).
- `./mvnw test` en verde antes de dar por terminada una tarea.
