package com.lombard.service;

import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.repository.AccountRepository;
import com.lombard.repository.AccountTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Тесты счета клиента: пополнение, снятие денег и оплата покупок. На счете тестового клиента изначально 1000 руб
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    private AccountService accountService;

    private Account testAccount;

    private Client testClient;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, accountTransactionRepository);
        testClient = new Client();
        testClient.setId(1L);
        testAccount = new Account(testClient);
        testAccount.setId(1L);
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    // пополнили на 50 на счете должно стать 1050 и появиться операция
    @Test
    void deposit_validAmount_increasesBalance() {
        when(accountRepository.findByClientIdForUpdate(1L)).thenReturn(Optional.of(testAccount));
        accountService.deposit(1L, new BigDecimal("50"), "test");
        verify(accountRepository).save(argThat(a -> a.getBalance().compareTo(new BigDecimal("1050")) == 0));
        verify(accountTransactionRepository).save(any());
    }

    // у нового клиента счета ещё нет создаем с нулем
    @Test
    void getOrCreate_noAccount_createsEmptyAccount() {
        when(accountRepository.findByClientId(1L)).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Account a = accountService.getOrCreateForClient(testClient);
        assertThat(a.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void deposit_nullAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, null, "test"));
    }

    @Test
    void deposit_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, BigDecimal.ZERO, "test"));
    }

    @Test
    void deposit_negativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, new BigDecimal("-100"), "test"));
    }

    @Test
    void deposit_accountNotFound_throwsException() {
        when(accountRepository.findByClientIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> accountService.deposit(1L, new BigDecimal("100"), "test"));
    }

    // снять 5000 при балансе 1000 нельзя
    @Test
    void withdraw_notEnoughMoney_throwsException() {
        when(accountRepository.findByClientIdForUpdate(1L)).thenReturn(Optional.of(testAccount));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1L, new BigDecimal("5000.00"), "test"));
    }

    @Test
    void purchase_enoughMoney_savesTransaction() {
        when(accountRepository.findByClientIdForUpdate(1L)).thenReturn(Optional.of(testAccount));
        accountService.purchase(1L, new BigDecimal("500"), "Order #1");
        verify(accountTransactionRepository).save(any());
        verify(accountRepository).save(any());
    }

    @Test
    void purchase_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1L, BigDecimal.ZERO, null));
    }

    @Test
    void purchase_notEnoughMoney_throwsException() {
        when(accountRepository.findByClientIdForUpdate(1L)).thenReturn(Optional.of(testAccount));
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1L, new BigDecimal("5000"), null));
    }
}
