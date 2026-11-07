package com.lombard.repository;

import com.lombard.model.Account;
import com.lombard.model.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Проверяем свой запрос репозитория счетов на базе H2
@DataJpaTest
class AccountRepositoryTest {

    @Autowired AccountRepository accountRepository;
    @Autowired TestEntityManager em;

    // счет ищется по клиенту с блокировкой строки (для пополнения/списания)
    @Test
    void findByClientIdForUpdate_existingAccount_found() {
        Client c = new Client(); c.setFullName("A B"); c.setPhone("+70000000001");
        em.persist(c);
        Account a = new Account(c); a.setBalance(new BigDecimal("100"));
        accountRepository.save(a);
        em.flush(); em.clear();

        assertThat(accountRepository.findByClientIdForUpdate(c.getId())).isPresent();
    }
}