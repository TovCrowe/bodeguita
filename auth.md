# Autenticación y membresías

Léelo cuando toques `auth/`, `household/` o el modelo de `User`.

## Login con Google + JWT propio

**Decidido:** la API emite su **propio JWT** (no valida ID tokens de Google en cada
request). Flujo:

1. El backend actúa como **OAuth2 Client** (`spring-boot-starter-oauth2-client`) y
   ejecuta el flujo *authorization code* con Google.
2. Tras el callback, busca al `User` por `googleSub`:
   - Existe → login normal.
   - No existe pero su `email` coincide con una **membresía pendiente** → se enlaza a
     esa familia y pasa a `ACTIVE` (ver *Membresías*).
   - No existe ni tiene invitación → se rechaza. *Excepción:* el primer usuario crea su
     propia familia y queda como `OWNER`.
3. El backend **emite un JWT firmado** (recomendado RS256; secreto/llave por env) con
   `userId`, `householdId` y `role`.
4. El cliente manda el JWT como `Authorization: Bearer ...`.
5. Spring Security lo valida como **resource server** en cada request.

Definir expiración razonable del access token y, si se quiere sesión larga, refresh
token. Secretos siempre por variables de entorno.

## Membresías (el owner agrega manual)

No hay auto-registro ni invitaciones por código/email. El `OWNER` agrega miembros:

1. Endpoint del owner (p. ej. `POST /household/members`) con el **email** del nuevo miembro.
2. Se crea un `User` en estado `PENDING`, ligado al household, sin `googleSub` todavía.
3. Cuando esa persona hace login con Google y su email coincide, se completa el `User`
   (`googleSub`, pasa a `ACTIVE`) y obtiene acceso al inventario familiar.

Solo el `OWNER` puede agregar/quitar miembros. El `email` es la clave que enlaza la
invitación pendiente con el login.

## Regla de seguridad clave

Toda operación sobre `PantryItem` se valida contra el `household` del usuario
autenticado. Un usuario **solo** ve o modifica el inventario de su propia familia.
La comprobación va en la capa **service**, no solo en el controlador.
