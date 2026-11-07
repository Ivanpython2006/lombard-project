package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.AccountRepository;
import com.lombard.repository.AccountTransactionRepository;
import com.lombard.repository.CartItemRepository;
import com.lombard.repository.ClientRepository;
import com.lombard.repository.CollateralRepository;
import com.lombard.repository.LoanHistoryRepository;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.NotificationRepository;
import com.lombard.repository.NumberSequenceRepository;
import com.lombard.repository.OrderItemRepository;
import com.lombard.repository.OrderRepository;
import com.lombard.repository.UserRepository;
import com.lombard.service.AccountService;
import com.lombard.service.CartService;
import com.lombard.service.ClientService;
import com.lombard.service.CollateralService;
import com.lombard.service.FileStorageService;
import com.lombard.service.LoanService;
import com.lombard.service.ManagerStatsService;
import com.lombard.service.NotificationService;
import com.lombard.service.NumberSequenceService;
import com.lombard.service.OrderService;
import com.lombard.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Общая основа интеграционных тестов

//Поднимается полный контекст Spring с базой H2 (настройки в src/test/resources/application.properties)
// доступны MockMvc, все репозитории и сервисы. Каждый тест выполняется в транзакции и откатывается поэтому тесты не влияют друг на друга

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected AccountTransactionRepository accountTransactionRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected CartService cartService;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected ClientService clientService;

    @Autowired
    protected CollateralRepository collateralRepository;

    @Autowired
    protected CollateralService collateralService;

    @Autowired
    protected FileStorageService fileStorageService;

    @Autowired
    protected LoanHistoryRepository loanHistoryRepository;

    @Autowired
    protected LoanRepository loanRepository;

    @Autowired
    protected LoanService loanService;

    @Autowired
    protected ManagerStatsService managerStatsService;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected NumberSequenceRepository numberSequenceRepository;

    @Autowired
    protected NumberSequenceService numberSequenceService;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderService orderService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserService userService;

    //стандартный набор данных

    // Администратор: логин "admin" пароль "pass"
    protected User admin;
    // Пользователь-клиент: логин "client1" пароль "pass"
    protected User clientUser;
    // Клиент C100 привязанный к {@link #clientUser}
    protected Client client;
    // Счет клиента с балансом 100 000
    protected Account account;
    // Заем L100 на 1 000 в статусе «В ожидании»
    protected Loan loan;
    // Залог M100 (оценка 5 000) привязанный к {@link #loan}
    protected Collateral collateral;
    // Оплаченный заказ O100 клиента на 5 000.
    protected Order order;

    // Очищает таблицы и создаёт стандартный набор данных: администратор клиент со счетом заем с залогом и оплаченный заказ

    protected void createDefaultData() {
        orderItemRepository.deleteAll();
        cartItemRepository.deleteAll();
        notificationRepository.deleteAll();
        loanHistoryRepository.deleteAll();
        collateralRepository.deleteAll();
        loanRepository.deleteAll();
        orderRepository.deleteAll();
        accountRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        numberSequenceRepository.deleteAll();

        admin = new User("admin", passwordEncoder.encode("pass"), "ADMIN");
        admin.setUserNumber("A1");
        userRepository.save(admin);

        clientUser = new User("client1", passwordEncoder.encode("pass"), "CLIENT");
        clientUser.setUserNumber("U_C1");
        userRepository.save(clientUser);

        client = new Client("Клиент", "+79990001122");
        client.setUser(clientUser);
        client.setClientNumber("C100");
        clientRepository.save(client);

        account = new Account(client);
        account.setBalance(new BigDecimal("100000"));
        accountRepository.save(account);

        loan = new Loan();
        loan.setAmount(new BigDecimal("1000"));
        loan.setStatus("В ожидании");
        loan.setCreatedDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusMonths(1));
        loan.setClient(client);
        loan.setLoanNumber("L100");
        loanRepository.save(loan);

        collateral = new Collateral("Товар", "desc", new BigDecimal("5000"));
        collateral.setItemNumber("M100");
        collateral.setPhotoUrls(new ArrayList<>());
        collateral.setLoan(loan);
        collateralRepository.save(collateral);
        loan.getCollaterals().add(collateral);
        loanRepository.save(loan);

        order = new Order();
        order.setClient(client);
        order.setTotalAmount(new BigDecimal("5000"));
        order.setStatus("Оплачен");
        order.setDeliveryType("PICKUP");
        order.setContactPhone("+79990001122");
        order.setOrderNumber("O100");
        orderRepository.save(order);
    }
}
