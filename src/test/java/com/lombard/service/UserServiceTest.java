package com.lombard.service;

import com.lombard.model.*;
import com.lombard.model.User;
import com.lombard.repository.*;
import com.lombard.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Тесты сервиса пользователей: регистрация, роли и пароли. Вместо мока PasswordEncoder используется настоящий BCrypt через @Spy.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private NumberSequenceService numberSequenceService;

    @Mock
    private UserRepository userRepository;

    // Настоящий BCrypt обернутый в spy: можно и проверять реальное шифрование и подменять ответы.
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, numberSequenceService);
    }

    @Test
    void delete_existingUser_deletes() {
        User u = new User(); u.setId(1L); u.setUsername("test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.delete(1L, "admin");
        verify(userRepository).deleteById(1L);
    }

    @Test
    void count_callsRepository() {
        when(userRepository.count()).thenReturn(10L);
        userService.count();
        verify(userRepository).count();
    }

    @Test
    void findAll_callsRepository() {
        when(userRepository.findAll()).thenReturn(List.of(new User()));
        userService.findAll();
        verify(userRepository).findAll();
    }

    @Test
    void changeRole_existingUser_setsNewRole() {
        User u = new User(); u.setId(1L); u.setRole("CLIENT");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.changeRole(1L, "MANAGER");

        assertThat(u.getRole()).isEqualTo("MANAGER");
        verify(userRepository).save(u);
    }

    @Test
    void changePassword_existingUser_savesEncodedPassword() {
        User u = new User(); u.setUsername("test");
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(u));
        doReturn("encodedNewPass").when(passwordEncoder).encode("newPass");

        userService.changePassword("test", "newPass");

        assertThat(u.getPassword()).isEqualTo("encodedNewPass");
        verify(userRepository).save(u);
    }

    @Test
    void checkPassword_encoderMatches_returnsTrue() {
        User u = new User(); u.setUsername("test"); u.setPassword("encoded");
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(u));
        doReturn(true).when(passwordEncoder).matches("raw", "encoded"); // doReturn, чтобы spy не вызывал настоящий BCrypt

        assertThat(userService.checkPassword("test", "raw")).isTrue();
    }

    // у каждой роли свой префикс номера: A, M, U
    @Test
    void register_admin_getsNumberA() {
        when(userRepository.findByUsername("a")).thenReturn(Optional.empty());
        when(numberSequenceService.nextNumber("A", "A")).thenReturn("A1");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User u = userService.register("a", "123456", "ADMIN");
        assertThat(u.getRole()).isEqualTo("ADMIN");
        assertThat(u.getUserNumber()).isEqualTo("A1");
    }

    @Test
    void register_manager_getsNumberM() {
        when(userRepository.findByUsername("m")).thenReturn(Optional.empty());
        when(numberSequenceService.nextNumber("M_USER", "M")).thenReturn("M1");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User u = userService.register("m", "123456", "MANAGER");
        assertThat(u.getRole()).isEqualTo("MANAGER");
    }

    @Test
    void register_client_getsNumberU() {
        when(userRepository.findByUsername("c")).thenReturn(Optional.empty());
        when(numberSequenceService.nextNumber("U_C", "U")).thenReturn("U1");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User u = userService.register("c", "123456", "CLIENT");
        assertThat(u.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void register_takenUsername_throwsException() {
        when(userRepository.findByUsername("c")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> userService.register("c", "123456", "CLIENT"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkPassword_rightPassword_returnsTrue() {
        User u = new User();
        u.setPassword(new BCryptPasswordEncoder().encode("pass"));
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(u));
        assertThat(userService.checkPassword("u", "pass")).isTrue();
    }

    @Test
    void checkPassword_wrongPassword_returnsFalse() {
        User u = new User();
        u.setPassword(new BCryptPasswordEncoder().encode("pass"));
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(u));
        assertThat(userService.checkPassword("u", "wrong")).isFalse();
    }
}
