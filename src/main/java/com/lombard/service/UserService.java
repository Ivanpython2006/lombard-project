package com.lombard.service;

import com.lombard.model.User;
import com.lombard.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "CLIENT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NumberSequenceService numberSequenceService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       NumberSequenceService numberSequenceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.numberSequenceService = numberSequenceService;
    }

    @Transactional
    public User register(String username, String password, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не короче 6 символов");
        }
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            throw new IllegalArgumentException("Недопустимая роль: " + role);
        }
        String normalizedRole = role.toUpperCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Логин уже занят");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(normalizedRole);

        String prefix;
        String letter;
        switch (normalizedRole) {
            case "ADMIN":
                prefix = "A"; letter = "A"; break;
            case "MANAGER":
                prefix = "M_USER"; letter = "M"; break;
            default:
                prefix = "U_C"; letter = "U"; break;
        }
        user.setUserNumber(numberSequenceService.nextNumber(prefix, letter));

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    public long count() {
        return userRepository.count();
    }

    @Transactional
    public void changeRole(Long id, String role) {
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            throw new IllegalArgumentException("Недопустимая роль: " + role);
        }
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setRole(role.toUpperCase());
            userRepository.save(user);
        }
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User target = userRepository.findById(id).orElse(null);
        if (target == null) return;

        if (currentUsername != null && currentUsername.equals(target.getUsername())) {
            throw new IllegalStateException("Нельзя удалить самого себя");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public boolean checkPassword(String username, String rawPassword) {
        User user = findByUsername(username);
        if (user == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не короче 6 символов");
        }
        User user = findByUsername(username);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }
}