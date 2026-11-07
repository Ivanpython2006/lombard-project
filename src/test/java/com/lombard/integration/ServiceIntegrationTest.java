package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Проверка сервисов вместе с настоящей базой H2 (без моков)
class ServiceIntegrationTest extends IntegrationTestBase {

    private Client client;

    private User user;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        notificationRepository.deleteAll();
        collateralRepository.deleteAll();
        loanRepository.deleteAll();
        orderRepository.deleteAll();
        accountRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        numberSequenceRepository.deleteAll();

        user = new User("client1", passwordEncoder.encode("pass"), "CLIENT");
        user.setUserNumber("U_C1");
        userRepository.save(user);

        client = new Client("Клиент", "+79990001122");
        client.setUser(user);
        client.setClientNumber("C100");
        clientRepository.save(client);
    }

    @Test
    void userService_realDatabase_works() {
        assertNotNull(userService.findByUsername("client1"));
        assertNotNull(userService.findById(user.getId()));
        assertFalse(userService.findAll().isEmpty());
        assertEquals(1, userService.findByRole("CLIENT").size());
        assertEquals(1, userService.count());
        assertTrue(userService.checkPassword("client1", "pass"));
        assertFalse(userService.checkPassword("client1", "wrong"));

        userService.changePassword("client1", "newpass123");
        assertTrue(userService.checkPassword("client1", "newpass123"));

        userService.changeRole(user.getId(), "MANAGER");
        assertEquals("MANAGER", userService.findById(user.getId()).getRole());
    }

    @Test
    void clientService_realDatabase_works() {
        assertNotNull(clientService.findById(client.getId()));
        assertNotNull(clientService.findByUserId(user.getId()));
        assertEquals(1, clientService.getAllClients().size());
        assertEquals(1, clientService.findAll().size());
        assertEquals(1, clientService.count());
        assertTrue(clientService.existsByPhone("+79990001122"));
        assertTrue(clientService.existsByEmail("nonexist@test.com") == false);
        assertEquals(1, clientService.searchClients("Клиент").size());
        assertEquals(1, clientService.searchByName("Клиент").size());
        assertEquals(1, clientService.searchByAnyField("+7999").size());
    }

    // пополнение 5000 снятие 1000 покупка 1000 => на счёте 3000
    @Test
    void accountService_depositWithdrawPurchase_changesBalance() {
        Account acc = accountService.getOrCreateForClient(client);
        assertNotNull(acc);
        assertNotNull(accountService.findByClientId(client.getId()));

        accountService.deposit(client.getId(), new BigDecimal("5000"), "test");
        Account updated = accountService.findByClientId(client.getId());
        assertEquals(0, new BigDecimal("5000").compareTo(updated.getBalance()));

        accountService.withdraw(client.getId(), new BigDecimal("1000"), "test");
        updated = accountService.findByClientId(client.getId());
        assertEquals(0, new BigDecimal("4000").compareTo(updated.getBalance()));

        accountService.purchase(client.getId(), new BigDecimal("1000"), "test");
        updated = accountService.findByClientId(client.getId());
        assertEquals(0, new BigDecimal("3000").compareTo(updated.getBalance()));

        assertFalse(accountService.getHistory(acc.getId()).isEmpty());
    }

    @Test
    void loanService_realDatabase_works() {
        Loan loan = new Loan();
        loan.setAmount(new BigDecimal("1000"));
        loan.setStatus("В ожидании");
        loan.setCreatedDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusMonths(1));
        loan.setClient(client);
        loan.setLoanNumber("L100");
        loanService.save(loan);

        assertNotNull(loanService.findById(loan.getId()));
        assertEquals(1, loanService.findAll().size());
        assertEquals(1, loanService.count());
        assertEquals(1, loanService.findByClientId(client.getId()).size());
        assertEquals(1, loanService.findPaginated(1, 10, "id").getTotalElements());
        assertEquals(1, loanService.findAllSorted("amount").size());

        assertNotNull(loanService.getAverageLoanAmount());
        assertNotNull(loanService.getAverageWaitingDays());
        assertFalse(loanService.countByStatus().isEmpty());
        assertFalse(loanService.sumByStatus().isEmpty());

        assertFalse(loanService.searchLoans("В ожидании", "L100", null).isEmpty());

        loanService.extendLoan(loan.getId(), 3);
        loanService.closeLoan(loan.getId());
        loanService.sellLoan(loan.getId());

        assertNotNull(loanService.getItemName(loan));
    }

    @Test
    void notificationService_markRead_unreadCountDrops() {
        notificationService.notify(client, "TEST", "INFO", "arg");
        assertFalse(notificationService.findByUser(user.getId()).isEmpty());
        assertEquals(1, notificationService.countUnread(user.getId()));

        notificationService.markAllRead(user.getId());
        assertEquals(0, notificationService.countUnread(user.getId()));
        assertFalse(notificationService.findReadByUser(user.getId()).isEmpty());
    }

    @Test
    void numberSequence_twoCalls_givesT1AndT2() {
        String n1 = numberSequenceService.nextNumber("TEST", "T");
        String n2 = numberSequenceService.nextNumber("TEST", "T");
        assertEquals("T1", n1);
        assertEquals("T2", n2);
    }

    @Test
    void cartService_addAndClear_works() {
        Loan loan = new Loan();
        loan.setAmount(new BigDecimal("1000"));
        loan.setStatus("На продаже");
        loan.setClient(client);
        loan.setLoanNumber("L200");
        loan.setCreatedDate(LocalDate.now());
        loanRepository.save(loan);

        cartService.add(client, loan);
        assertEquals(1, cartService.count(client.getId()));
        assertTrue(cartService.contains(client.getId(), loan.getId()));
        assertFalse(cartService.getByClient(client.getId()).isEmpty());

        CartItem item = cartService.getByClient(client.getId()).get(0);
        cartService.remove(item.getId(), client.getId());
        assertEquals(0, cartService.count(client.getId()));
    }

    @Test
    void managerStats_afterAction_countsIt() {
        Loan loan = new Loan();
        loan.setAmount(new BigDecimal("1000"));
        loan.setStatus("Выдан");
        loan.setClient(client);
        loan.setLoanNumber("L300");
        loan.setCreatedDate(LocalDate.now());
        loanRepository.save(loan);

        managerStatsService.getAllActions("admin");
        managerStatsService.getStatsThisMonth("admin");
        managerStatsService.getStatsAllTime("admin");
        assertNotNull(managerStatsService.getStatsForPeriod("admin",
                LocalDate.now().minusDays(1).atStartOfDay(),
                LocalDate.now().plusDays(1).atTime(23, 59)));
    }
}
