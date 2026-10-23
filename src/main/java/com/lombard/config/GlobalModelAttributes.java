package com.lombard.config;

import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.CartService;
import com.lombard.service.ClientService;
import com.lombard.service.LoanService;
import com.lombard.service.NotificationService;
import com.lombard.service.OrderService;
import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final ClientService clientService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final LoanService loanService;
    private final OrderService orderService;

    public GlobalModelAttributes(UserService userService, ClientService clientService,
                                 CartService cartService, NotificationService notificationService,
                                 LoanService loanService, OrderService orderService) {
        this.userService = userService;
        this.clientService = clientService;
        this.cartService = cartService;
        this.notificationService = notificationService;
        this.loanService = loanService;
        this.orderService = orderService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return;

        // для клиента
        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        if (isClient) {
            User user = userService.findByUsername(auth.getName());
            if (user != null) {
                Client client = clientService.findByUserId(user.getId());
                if (client != null) {
                    long cartCount = cartService.count(client.getId());
                    model.addAttribute("cartCount", cartCount);

                    long unreadNotifications = notificationService.countUnread(user.getId());
                    model.addAttribute("unreadNotifications", unreadNotifications);

                    List<Loan> loans = loanService.findByClientId(client.getId());
                    long activeLoans = loans.stream()
                            .filter(l -> "Выдан".equals(l.getStatus()) || "Просрочен".equals(l.getStatus()))
                            .count();
                    model.addAttribute("activeLoansCount", activeLoans);
                }
            }
        }

        //для админа и менеджера
        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
        if (isStaff) {
            List<Loan> allLoans = loanService.findAll();

            long pendingIntake = allLoans.stream()
                    .filter(l -> "В ожидании".equals(l.getStatus()))
                    .count();
            model.addAttribute("pendingIntakeCount", pendingIntake);

            long pendingLoans = allLoans.stream()
                    .filter(l -> "Ожидает подтверждения".equals(l.getStatus())
                            || "Просрочен".equals(l.getStatus()))
                    .count();
            model.addAttribute("pendingLoansCount", pendingLoans);

            long warehouseCount = allLoans.stream()
                    .filter(l -> "На складе".equals(l.getStatus()))
                    .count();
            model.addAttribute("warehouseCount", warehouseCount);

            long pendingOrders = orderService.findAll().stream()
                    .filter(o -> "Оплачен".equals(o.getStatus()))
                    .count();
            model.addAttribute("pendingOrdersCount", pendingOrders);
        }
    }
}