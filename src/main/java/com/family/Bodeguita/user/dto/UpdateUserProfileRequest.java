package com.family.Bodeguita.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Los únicos datos de perfil que el propio usuario puede cambiar. */
public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 512) String avatarUrl) {
}
