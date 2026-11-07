package com.lombard.controller;

import com.lombard.service.AccountService;
import com.lombard.service.CartService;
import com.lombard.service.ClientService;
import com.lombard.service.CollateralService;
import com.lombard.service.FileStorageService;
import com.lombard.service.LoanHistoryService;
import com.lombard.service.LoanService;
import com.lombard.service.ManagerStatsService;
import com.lombard.service.NotificationService;
import com.lombard.service.NumberSequenceService;
import com.lombard.service.OrderService;
import com.lombard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

// Общая основа для срезовых тестов контроллеров (@WebMvcTest)
// Поднимается только веб-слой; все сервисы заменены заглушками Mockito

abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected AccountService accountService;

    @MockBean
    protected CartService cartService;

    @MockBean
    protected ClientService clientService;

    @MockBean
    protected CollateralService collateralService;

    @MockBean
    protected FileStorageService fileStorageService;

    @MockBean
    protected LoanHistoryService loanHistoryService;

    @MockBean
    protected LoanService loanService;

    @MockBean
    protected ManagerStatsService managerStatsService;

    @MockBean
    protected NotificationService notificationService;

    @MockBean
    protected NumberSequenceService numberSequenceService;

    @MockBean
    protected OrderService orderService;

    @MockBean
    protected UserService userService;
}
