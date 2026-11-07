package com.lombard.repository;

import com.lombard.model.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

// Проверяем свои запросы репозитория клиентов на базе H2
@DataJpaTest
class ClientRepositoryTest {

    @Autowired ClientRepository clientRepository;

    @Test
    void searchByAnyField_byLastName_findsClient() {
        Client c = new Client();
        c.setFullName("Ivanov Ivan"); c.setPhone("+79991112233");
        clientRepository.save(c);
        assertThat(clientRepository.searchByAnyField("Ivanov")).isNotEmpty();
    }

    @Test
    void existsByPhone_savedPhone_returnsTrue() {
        Client c = new Client();
        c.setFullName("Petrov Pet"); c.setPhone("+79995556677");
        clientRepository.save(c);
        assertThat(clientRepository.existsByPhone("+79995556677")).isTrue();
    }
}