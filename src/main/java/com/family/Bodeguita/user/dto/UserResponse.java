package com.family.Bodeguita.user.dto;

import com.family.Bodeguita.user.domain.Role;
import java.time.Instant;

/**
 * Vista pública de un miembro. No expone {@code googleSub} —es del proveedor, no del
 * perfil— ni {@code emailCanonical}, que es una clave interna de comparación.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        Long householdId,
        Role role,
        Instant createdAt) {
}
