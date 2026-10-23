package com.lombard.controller;

import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.AccountService;
import com.lombard.service.ClientService;
import com.lombard.service.LoanService;
import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private final UserService userService;
    private final ClientService clientService;
    private final LoanService loanService;
    private final AccountService accountService;

    public HomeController(UserService userService,
                          ClientService clientService,
                          LoanService loanService,
                          AccountService accountService) {
        this.userService = userService;
        this.clientService = clientService;
        this.loanService = loanService;
        this.accountService = accountService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication auth) {
        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (!isClient) return "index";

        User user = userService.findByUsername(auth.getName());
        if (user == null) return "index";

        Client client = clientService.findByUserId(user.getId());
        if (client == null) return "index";

        Account account = accountService.getOrCreateForClient(client);
        model.addAttribute("clientBalance", account.getBalance());

        List<Loan> allLoans = loanService.findByClientId(client.getId());
        List<Loan> activeLoans = new ArrayList<>();
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (Loan l : allLoans) {
            if ("Выдан".equals(l.getStatus()) || "Просрочен".equals(l.getStatus())) {
                activeLoans.add(l);
                totalDebt = totalDebt.add(l.getTotalToReturn());
            }
        }

        model.addAttribute("activeLoansCount", activeLoans.size());
        model.addAttribute("totalDebt", totalDebt);

        activeLoans.sort(Comparator.comparing(Loan::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<Loan> urgent = activeLoans.size() > 3
                ? activeLoans.subList(0, 3)
                : activeLoans;

        model.addAttribute("urgentLoans", urgent);

        return "index";
    }
}