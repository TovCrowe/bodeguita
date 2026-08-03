# Autenticación y membresías

Léelo cuando toques `auth/`, `household/` o el modelo de `User`.

## Login con Google + JWT propio

**Decidido:** la API emite su **propio JWT** (no valida ID tokens de Google en cada
request). Flujo:

1. El backend actúa como **OAuth2 Client** (`spring-boot-starter-oauth2-client`) y
   ejecuta el flujo *authorization code* con Google.
2. Tras el callback, busca al `User` por `googleSub`:
   - Existe → login normal.
   - No existe pero hay una **invitación vigente** para su email → `acceptInvitation`
     lo da de alta como `MEMBER` de esa familia (ver *Membresías*).
   - No existe ni tiene invitación → se rechaza (`NotInvitedException` → 403).
     *Excepción:* el primer usuario crea su propia familia y queda como `OWNER`
     (`createWithOwner`).
3. El backend **emite un JWT firmado** (recomendado RS256; secreto/llave por env) con
   `userId`, `householdId` y `role`.
4. El cliente manda el JWT como `Authorization: Bearer ...`.
5. Spring Security lo valida como **resource server** en cada request.

Definir expiración razonable del access token y, si se quiere sesión larga, refresh
token. Secretos siempre por variables de entorno.

### Obligatorio: `email_verified`

El email es lo único que enlaza una invitación con una cuenta de Google, así que
**`acceptInvitation` solo se puede llamar con un email cuyo `email_verified` del ID token
sea `true`**. Una cuenta de Google Workspace puede llevar el email que su admin de dominio
quiera; sin esa comprobación, cualquier invitación sería reclamable por quien no debe.

## Membresías: invitación explícita

No hay auto-registro. El `OWNER` invita, y la invitación es una entidad propia
(`household/domain/Invitation`), **no** un `User` a medio hacer:

1. `POST` del owner con el **email** del invitado → `HouseholdService.inviteMember`.
2. Se crea una `Invitation` en estado `PENDING` con caducidad
   (`bodeguita.invitation.ttl`, 7 días por defecto).
3. Cuando esa persona entra con Google, `claim` consume la invitación (pasa a `ACCEPTED`)
   y se crea el `User` como `MEMBER` de esa familia.

Un `User` es **siempre** una persona ya autenticada: `googleSub` nunca es nulo y no hay
estados. Esa fue la razón de separar la invitación — antes un `PENDING` sin `googleSub`
ocupaba una fila de `users` y no había forma de caducarla, revocarla ni reenviarla.

### Reglas

- Solo el `OWNER` invita, revoca y lista invitaciones pendientes.
- **Reinvitar es reenviar:** volver a invitar el mismo email en la misma familia renueva
  la caducidad en lugar de crear una segunda invitación.
- Como mucho **una invitación `PENDING` por email en todo el sistema**, garantizado por un
  índice único parcial (`WHERE status = 'PENDING'`). Así al hacer login hay a lo sumo una
  invitación que reclamar y aceptar es inequívoco.
- La caducidad se marca **de forma perezosa** (al intentar aceptar o al reinvitar ese
  email), no con un job: una invitación vencida no bloquea reintentar con el email bueno.
- Estados: `PENDING → ACCEPTED | REVOKED | EXPIRED`. Nunca se vuelve a `PENDING`.
- Un email que ya es `User` no se puede invitar: `AlreadyMemberException` si es de esta
  familia, `EmailBelongsToAnotherHouseholdException` si es de otra — sin decir de cuál.

### Email: dos formas

Google trata `juan.perez@gmail.com`, `juanperez@gmail.com` y `juan.perez+casa@gmail.com`
como **la misma cuenta**. Si se comparan como cadenas, invitar con puntos y entrar sin
ellos deja al miembro fuera sin explicación. Por eso `common/EmailNormalizer` produce dos
formas, y ambas se guardan tanto en `users` como en `household_invitation`:

- `email` — lo que escribió el OWNER (trim + minúsculas). Solo para mostrar.
- `email_canonical` — clave de comparación: en Gmail/googlemail se quitan los puntos y el
  `+tag`. **Es la que lleva el índice único y por la que siempre se busca.**

Nunca buscar por `email`. Nunca tocar una de las dos por separado: van juntas en
`User.changeEmail` y en `Invitation.pending`.

## Regla de seguridad clave

Toda operación sobre `PantryItem` se valida contra el `household` del usuario
autenticado. Un usuario **solo** ve o modifica el inventario de su propia familia.
La comprobación va en la capa **service**, no solo en el controlador.

Los checks de pertenencia y de rol (`requireMember`, `requireOwner`) viven en
`HouseholdServiceImpl`; `UserService` e `InvitationService` no deciden sobre membresías,
solo ejecutan.
