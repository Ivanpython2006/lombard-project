package com.lombard.service;

import com.lombard.model.Account;
import com.lombard.model.AccountTransaction;
import com.lombard.model.Client;
import com.lombard.repository.AccountRepository;
import com.lombard.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository txRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountTransactionRepository txRepository) {
        this.accountRepository = accountRepository;
        this.txRepository = txRepository;
    }

    @Transactional
    public Account getOrCreateForClient(Client client) {
        return accountRepository.findByClientId(client.getId())
                .orElseGet(() -> {
                    Account a = new Account(client);
                    return accountRepository.save(a);
                });
    }

    public Account findByClientId(Long clientId) {
        return accountRepository.findByClientId(clientId).orElse(null);
    }

    public List<AccountTransaction> getHistory(Long accountId) {
        return txRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional
    public void deposit(Long clientId, BigDecimal amount, String comment) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть больше 0");
        }
        Account account = accountRepository.findByClientIdForUpdate(clientId)
                .orElseThrow(() -> new IllegalStateException("Счет не найден"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        txRepository.save(new AccountTransaction(
                account, amount, "DEPOSIT",
                comment != null && !comment.isEmpty() ? comment : "Пополнение счета"
        ));
    }

    @Transactional
    public void withdraw(Long clientId, BigDecimal amount, String comment) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма вывода должна быть больше 0");
        }
        Account account = accountRepository.findByClientIdForUpdate(clientId)
                .orElseThrow(() -> new IllegalStateException("Счет не найден"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Недостаточно средств на счете");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        txRepository.save(new AccountTransaction(
                account, amount, "WITHDRAW",
                comment != null && !comment.isEmpty() ? comment : "Вывод средств"
        ));
    }

    @Transactional
    public void purchase(Long clientId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма покупки должна быть больше 0");
        }
        Account account = accountRepository.findByClientIdForUpdate(clientId)
                .orElseThrow(() -> new IllegalStateException("Счет не найден"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Недостаточно средств для покупки");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        txRepository.save(new AccountTransaction(
                account, amount, "PURCHASE",
                description != null ? description : "Покупка на витрине"
        ));
    }
}