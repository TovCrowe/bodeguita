package com.family.Bodeguita.user.service;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.dto.UpdateUserProfileRequest;
import java.util.List;
import java.util.Optional;

/**
 * Ciclo de vida del miembro. No decide sobre membresías: quién puede invitar o quitar a
 * quién es responsabilidad de {@code HouseholdService}, que es el que llama aquí.
 *
 * <p>Todo {@code User} que se crea aquí ya viene autenticado con Google. Lo que aún no lo
 * está no es un usuario todavía: es una {@code Invitation}.
 */
public interface UserService {

    User getById(Long id);

    Optional<User> findByGoogleSub(String googleSub);

    /** Acepta el email en cualquier forma; compara por la clave canónica. */
    Optional<User> findByEmail(String email);

    List<User> findAllByHousehold(Long householdId);

    /** Alta del dueño de una familia recién creada. */
    User createOwner(Household household, String googleSub, String email, String name, String avatarUrl);

    /** Alta de quien acaba de aceptar una invitación en su primer login. */
    User createMember(Household household, String googleSub, String email, String name, String avatarUrl);

    User updateProfile(Long userId, UpdateUserProfileRequest request);

    void delete(User user);
}
