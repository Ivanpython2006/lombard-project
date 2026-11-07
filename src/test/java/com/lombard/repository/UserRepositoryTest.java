package com.lombard.repository;

import com.lombard.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

//Проверка поиска пользователей по логину и по роли

@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    @Test
    void findByUsername_existing_found() {
        User u = new User(); u.setUsername("u1"); u.setPassword("p"); u.setRole("CLIENT");
        userRepository.save(u);
        assertThat(userRepository.findByUsername("u1")).isPresent();
    }

    @Test
    void findByRole_manager_returnsManagers() {
        User u = new User(); u.setUsername("u2"); u.setPassword("p"); u.setRole("MANAGER");
        userRepository.save(u);
        assertThat(userRepository.findByRole("MANAGER")).hasSize(1);
    }
}