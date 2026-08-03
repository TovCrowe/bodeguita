package com.family.Bodeguita.household.dto;

import com.family.Bodeguita.household.domain.InvitationStatus;
import java.time.Instant;

/** Vista de una invitación para el OWNER. El email es el que él escribió, no el canónico. */
public record InvitationResponse(
        Long id,
        String email,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt) {
}
