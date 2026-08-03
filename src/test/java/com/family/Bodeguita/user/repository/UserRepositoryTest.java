package com.family.Bodeguita.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.repository.HouseholdRepository;
import com.family.Bodeguita.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Usa el H2 del perfil test (MODE=PostgreSQL), no el embebido que @DataJpaTest pondría por defecto. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    private Household losTorres;
    private Household otraFamilia;

    @BeforeEach
    void setUp() {
        losTorres = householdRepository.save(new Household("Los Torres"));
        otraFamilia = householdRepository.save(new Household("Otra familia"));
    }

    @Test
    @DisplayName("findByEmailCanonical encuentra al usuario aunque el alias de Gmail sea otro")
    void findByEmailCanonical() {
        userRepository.save(
                User.member(losTorres, "google-456", "Juan.Perez@Gmail.com", "Juan", null));

        assertThat(userRepository.findByEmailCanonical("juanperez@gmail.com")).isPresent();
        assertThat(userRepository.existsByEmailCanonical("juanperez@gmail.com")).isTrue();
        assertThat(userRepository.existsByEmailCanonical("juan.perez@gmail.com")).isFalse();
    }

    @Test
    @DisplayName("findByGoogleSub localiza al usuario por su cuenta de Google")
    void findByGoogleSub() {
        userRepository.save(User.owner(losTorres, "google-123", "ana@example.com", "Ana", null));

        assertThat(userRepository.findByGoogleSub("google-123"))
                .get()
                .extracting(User::getEmail)
                .isEqualTo("ana@example.com");
        assertThat(userRepository.findByGoogleSub("google-999")).isEmpty();
    }

    @Test
    @DisplayName("findAllByHouseholdId no filtra usuarios de otra familia")
    void findAllByHouseholdIdIsolatesHouseholds() {
        userRepository.save(User.owner(losTorres, "google-1", "ana@example.com", "Ana", null));
        userRepository.save(User.member(losTorres, "google-2", "beto@example.com", "Beto", null));
        userRepository.save(User.owner(otraFamilia, "google-3", "carla@example.com", "Carla", null));

        assertThat(userRepository.findAllByHouseholdId(losTorres.getId()))
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("ana@example.com", "beto@example.com");
    }

    @Test
    @DisplayName("google_sub es único: no puede haber dos usuarios con la misma cuenta de Google")
    void googleSubIsUnique() {
        userRepository.saveAndFlush(
                User.owner(losTorres, "google-123", "ana@example.com", "Ana", null));

        User duplicado = User.owner(otraFamilia, "google-123", "otra@example.com", "Otra", null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("email_canonical es único: una persona pertenece a una sola familia")
    void emailCanonicalIsUnique() {
        userRepository.saveAndFlush(
                User.owner(losTorres, "google-123", "juan.perez@gmail.com", "Juan", null));

        // Mismo buzón real escrito distinto: el único tiene que verlo como el mismo.
        User duplicado =
                User.owner(otraFamilia, "google-999", "juanperez+casa@gmail.com", "Juan", null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
