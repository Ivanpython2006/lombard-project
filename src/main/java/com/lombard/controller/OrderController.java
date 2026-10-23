package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.Order;
import com.lombard.model.User;
import com.lombard.service.ClientService;
import com.lombard.service.OrderService;
import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/client/orders")
public class OrderController {

    private final OrderService orderService;
    private final ClientService clientService;
    private final UserService userService;

    public OrderController(OrderService orderService,
                           ClientService clientService,
                           UserService userService) {
        this.orderService = orderService;
        this.clientService = clientService;
        this.userService = userService;
    }

    private Client getCurrentClient(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (user == null) return null;
        return clientService.findByUserId(user.getId());
    }

    @GetMapping
    public String list(@RequestParam(required = false) String delivery,
                       @RequestParam(defaultValue = "date_desc") String sort,
                       @RequestParam(required = false) String query,
                       Model model, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        List<Order> orders = orderService.findByClientId(client.getId());

        if (delivery != null && !delivery.isEmpty()) {
            orders.removeIf(o -> !delivery.equals(o.getDeliveryType()));
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase().trim();
            orders.removeIf(o -> {
                if (o.getOrderNumber() != null
                        && o.getOrderNumber().toLowerCase().contains(q)) return false;

                boolean matchItem = o.getItems().stream()
                        .anyMatch(i -> i.getItemName() != null
                                && i.getItemName().toLowerCase().contains(q));
                return !matchItem;
            });
        }

        Comparator<Order> cmp;
        switch (sort) {
            case "date_asc":
                cmp = Comparator.comparing(
                        o -> o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.MIN,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo));
                break;
            case "number":
                cmp = Comparator.comparing(
                        o -> o.getOrderNumber() != null ? o.getOrderNumber() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "total":
                cmp = Comparator.comparing(
                        o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                        Comparator.nullsLast(BigDecimal::compareTo));
                break;
            default:
                cmp = Comparator.comparing(
                        (Order o) -> o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.MIN,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)).reversed();
        }
        orders.sort(cmp);

        model.addAttribute("orders", orders);
        model.addAttribute("filterDelivery", delivery);
        model.addAttribute("sort", sort);
        model.addAttribute("query", query);
        return "client_orders";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model,
                          Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        Order order = orderService.findById(id);
        if (order == null || order.getClient() == null
                || !order.getClient().getId().equals(client.getId())) {
            ra.addFlashAttribute("error", "Заказ не найден");
            return "redirect:/client/orders";
        }
        model.addAttribute("order", order);
        return "client_order_details";
    }
}