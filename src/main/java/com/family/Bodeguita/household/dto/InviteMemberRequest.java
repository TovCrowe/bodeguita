package com.family.Bodeguita.household.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El OWNER invita por email; ese email es lo que después enlaza con el login de Google. */
public record InviteMemberRequest(@NotBlank @Email @Size(max = 255) String email) {
}
