package com.family.Bodeguita.household.mapper;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.dto.HouseholdResponse;
import org.springframework.stereotype.Component;

@Component
public class HouseholdMapper {

    public HouseholdResponse toResponse(Household household) {
        return new HouseholdResponse(
                household.getId(), household.getName(), household.getCreatedAt());
    }
}
