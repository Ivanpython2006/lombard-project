package com.lombard.controller;

import com.lombard.model.Order;
import com.lombard.service.NotificationService;
import com.lombard.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminOrderController {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("Оплачен", "Собран", "Доставлен", "Отменён");

    private final OrderService orderService;
    private final NotificationService notificationService;

    public AdminOrderController(OrderService orderService,
                                NotificationService notificationService) {
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String delivery,
                       @RequestParam(defaultValue = "date_desc") String sort,
                       @RequestParam(required = false) String query,
                       Model model) {
        List<Order> orders = orderService.findAll();

        if (status != null && !status.isEmpty()) {
            orders.removeIf(o -> !status.equals(o.getStatus()));
        }
        if (delivery != null && !delivery.isEmpty()) {
            orders.removeIf(o -> !delivery.equals(o.getDeliveryType()));
        }
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase().trim();
            orders.removeIf(o -> {
                boolean matchNumber = o.getOrderNumber() != null
                        && o.getOrderNumber().toLowerCase().contains(q);
                boolean matchClient = o.getClient() != null
                        && o.getClient().getFullName() != null
                        && o.getClient().getFullName().toLowerCase().contains(q);
                boolean matchPhone = o.getContactPhone() != null
                        && o.getContactPhone().contains(q);
                boolean matchClientNumber = o.getClient() != null
                        && o.getClient().getClientNumber() != null
                        && o.getClient().getClientNumber().toLowerCase().contains(q);
                return !(matchNumber || matchClient || matchPhone || matchClientNumber);
            });
        }

        Comparator<Order> cmp;
        switch (sort) {
            case "number":
                cmp = Comparator.comparing(
                        o -> o.getOrderNumber() != null ? o.getOrderNumber() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "client":
                cmp = Comparator.comparing(
                        o -> o.getClient() != null && o.getClient().getFullName() != null
                                ? o.getClient().getFullName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "total":
                cmp = Comparator.comparing(
                        o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                        Comparator.nullsLast(BigDecimal::compareTo));
                break;
            case "date_asc":
                cmp = Comparator.comparing(
                        o -> o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.MIN,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo));
                break;
            default:
                cmp = Comparator.comparing(
                        (Order o) -> o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.MIN,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)).reversed();
        }
        orders.sort(cmp);

        model.addAttribute("orders", orders);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterDelivery", delivery);
        model.addAttribute("sort", sort);
        model.addAttribute("query", query);
        return "admin_orders";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Order order = orderService.findById(id);
        if (order == null) {
            ra.addFlashAttribute("error", "Заказ не найден");
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        return "admin_order_details";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes ra) {
        Order order = orderService.findById(id);
        if (order == null) {
            ra.addFlashAttribute("error", "Заказ не найден");
            return "redirect:/admin/orders";
        }

        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            ra.addFlashAttribute("error", "Недопустимый статус заказа: " + status);
            return "redirect:/admin/orders/" + id;
        }

        String oldStatus = order.getStatus();
        order.setStatus(status);
        orderService.save(order);

        if (order.getClient() != null) {
            String eventType;
            String type;
            switch (status) {
                case "Собран":
                    eventType = "ORDER_ASSEMBLED";
                    type = "INFO";
                    break;
                case "Доставлен":
                    eventType = "ORDER_DELIVERED";
                    type = "SUCCESS";
                    break;
                case "Отменён":
                    eventType = "ORDER_CANCELLED";
                    type = "DANGER";
                    break;
                default:
                    eventType = "ORDER_STATUS";
                    type = "INFO";
            }

            String deliveryText = "PICKUP".equals(order.getDeliveryType())
                    ? "самовывоз"
                    : "доставка";

            notificationService.notify(order.getClient(), eventType, type,
                    order.getOrderNumber(),
                    status,
                    deliveryText
            );
        }

        ra.addFlashAttribute("success", "Статус изменён: «" + oldStatus + "» → «" + status + "»");
        return "redirect:/admin/orders/" + id;
    }
}