package com.family.Bodeguita.household.dto;

import java.time.Instant;

public record HouseholdResponse(Long id, String name, Instant createdAt) {
}
