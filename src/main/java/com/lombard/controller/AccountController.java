package com.lombard.controller;

import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.AccountService;
import com.lombard.service.ClientService;
import com.lombard.service.NotificationService;
import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/client/account")
public class AccountController {

    private final AccountService accountService;
    private final ClientService clientService;
    private final UserService userService;
    private final NotificationService notificationService;

    public AccountController(AccountService accountService,
                             ClientService clientService,
                             UserService userService,
                             NotificationService notificationService) {
        this.accountService = accountService;
        this.clientService = clientService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    private Client getCurrentClient(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (user == null) return null;
        return clientService.findByUserId(user.getId());
    }

    @GetMapping
    public String accountPage(Model model, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        Account account = accountService.getOrCreateForClient(client);

        model.addAttribute("client", client);
        model.addAttribute("account", account);
        model.addAttribute("transactions", accountService.getHistory(account.getId()));
        return "client_account";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount,
                          @RequestParam(required = false) String comment,
                          Authentication auth,
                          RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        try {
            accountService.deposit(client.getId(), amount, comment);

            notificationService.notify(client, "ACCOUNT_DEPOSIT", "SUCCESS",
                    amount.toPlainString(),
                    (comment != null && !comment.isBlank() ? "Комментарий: " + comment : "")
            );

            ra.addFlashAttribute("success", "Счёт пополнен на " + amount.toPlainString() + " ₽");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/account";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam BigDecimal amount,
                           @RequestParam(required = false) String comment,
                           Authentication auth,
                           RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        try {
            accountService.withdraw(client.getId(), amount, comment);

            notificationService.notify(client, "ACCOUNT_WITHDRAW", "INFO",
                    amount.toPlainString(),
                    (comment != null && !comment.isBlank() ? "Комментарий: " + comment : "")
            );

            ra.addFlashAttribute("success", "Выведено " + amount.toPlainString() + " ₽");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/account";
    }
}