package com.family.Bodeguita.household.repository;

import com.family.Bodeguita.household.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
}
