package com.family.Bodeguita.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.user.domain.Role;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.dto.UpdateUserProfileRequest;
import com.family.Bodeguita.user.exception.EmailAlreadyRegisteredException;
import com.family.Bodeguita.user.exception.UserNotFoundException;
import com.family.Bodeguita.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;
    private Household household;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
        household = new Household("Los Torres");
        household.setId(1L);
    }

    @Test
    @DisplayName("getById lanza UserNotFoundException si el usuario no existe")
    void getByIdMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(42L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("createOwner deja al usuario con rol OWNER y su googleSub")
    void createOwner() {
        when(userRepository.existsByEmailCanonical("ana@example.com")).thenReturn(false);
        savesWhateverItGets();

        User owner = userService.createOwner(
                household, "google-123", " Ana@Example.com ", "Ana", "http://avatar");

        assertThat(owner.getRole()).isEqualTo(Role.OWNER);
        assertThat(owner.getGoogleSub()).isEqualTo("google-123");
        assertThat(owner.getEmail()).isEqualTo("ana@example.com");
        assertThat(owner.getHousehold()).isSameAs(household);
    }

    @Test
    @DisplayName("createMember deja al usuario con rol MEMBER")
    void createMember() {
        when(userRepository.existsByEmailCanonical("beto@example.com")).thenReturn(false);
        savesWhateverItGets();

        User member = userService.createMember(
                household, "google-456", "beto@example.com", "Beto", null);

        assertThat(member.getRole()).isEqualTo(Role.MEMBER);
        assertThat(member.getGoogleSub()).isEqualTo("google-456");
    }

    @Test
    @DisplayName("el email se guarda en dos formas: la que se muestra y la canónica")
    void emailIsStoredTwice() {
        when(userRepository.existsByEmailCanonical("juanperez@gmail.com")).thenReturn(false);
        savesWhateverItGets();

        User member = userService.createMember(
                household, "google-456", "Juan.Perez+casa@Gmail.com", "Juan", null);

        assertThat(member.getEmail()).isEqualTo("juan.perez+casa@gmail.com");
        assertThat(member.getEmailCanonical()).isEqualTo("juanperez@gmail.com");
    }

    @Test
    @DisplayName("el duplicado se detecta por la clave canónica, no por la cadena")
    void createRejectsCanonicalDuplicate() {
        when(userRepository.existsByEmailCanonical("juanperez@gmail.com")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userService.createMember(
                                        household, "google-456", "juan.perez@gmail.com", "Juan", null))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByEmail busca por la clave canónica")
    void findByEmailUsesTheCanonicalKey() {
        User juan = User.member(household, "google-456", "juan.perez@gmail.com", "Juan", null);
        when(userRepository.findByEmailCanonical("juanperez@gmail.com"))
                .thenReturn(Optional.of(juan));

        assertThat(userService.findByEmail("JuanPerez+trabajo@googlemail.com")).contains(juan);
    }

    @Test
    @DisplayName("updateProfile solo cambia nombre y avatar")
    void updateProfile() {
        User user = User.owner(household, "google-123", "ana@example.com", "Ana", null);
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        savesWhateverItGets();

        userService.updateProfile(7L, new UpdateUserProfileRequest("Ana Torres", "http://nuevo"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ana Torres");
        assertThat(saved.getValue().getAvatarUrl()).isEqualTo("http://nuevo");
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.OWNER);
    }

    private void savesWhateverItGets() {
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }
}
