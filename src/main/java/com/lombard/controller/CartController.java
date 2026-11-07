package com.lombard.controller;

import com.lombard.controller.dto.CheckoutForm;
import com.lombard.model.*;
import com.lombard.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.lombard.util.TransactionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/client/cart")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final AccountService accountService;
    private final ClientService clientService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final LoanService loanService;

    public CartController(CartService cartService,
                          OrderService orderService,
                          AccountService accountService,
                          ClientService clientService,
                          UserService userService,
                          NotificationService notificationService,
                          LoanService loanService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.accountService = accountService;
        this.clientService = clientService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.loanService = loanService;
    }

    private Client getCurrentClient(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (user == null) return null;
        return clientService.findByUserId(user.getId());
    }

    @GetMapping
    public String cart(Model model, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        List<CartItem> items = cartService.getByClient(client.getId());
        Account account = accountService.getOrCreateForClient(client);
        if (account == null) account = new Account(client); // на всякий случай: пустой счёт с нулём

        BigDecimal total = items.stream()
                .map(ci -> {
                    if (!ci.getLoan().getCollaterals().isEmpty()) {
                        Collateral c = ci.getLoan().getCollaterals().get(0);
                        return c.getEstimatedPrice() != null ? c.getEstimatedPrice() : BigDecimal.ZERO;
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean canPay = account.getBalance() != null
                && account.getBalance().compareTo(total) >= 0;

        model.addAttribute("client", client);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("account", account);
        model.addAttribute("canPay", canPay);
        return "client_cart";
    }

    @PostMapping("/add/{loanId}")
    public String add(@PathVariable Long loanId, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        try {
            Loan loan = loanService.findById(loanId);
            if (loan == null) {
                ra.addFlashAttribute("error", "Товар не найден");
                return "redirect:/shop";
            }
            cartService.add(client, loan);
            ra.addFlashAttribute("success", "Товар добавлен в корзину");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shop";
    }

    @PostMapping("/remove/{id}")
    public String remove(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        cartService.remove(id, client.getId());
        ra.addFlashAttribute("success", "Товар удалён из корзины");
        return "redirect:/client/cart";
    }

    @PostMapping("/checkout")
    @Transactional
    public String checkout(@Valid @ModelAttribute("form") CheckoutForm form,
                           BindingResult result,
                           Authentication auth,
                           RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        if (result.hasErrors()) {
            String msg = result.getFieldError() != null
                    ? result.getFieldError().getDefaultMessage() : "Ошибка валидации";
            ra.addFlashAttribute("error", msg);
            return "redirect:/client/cart";
        }

        List<CartItem> items = cartService.getByClient(client.getId());
        if (items.isEmpty()) {
            ra.addFlashAttribute("error", "Корзина пуста");
            return "redirect:/client/cart";
        }

        if ("DELIVERY".equals(form.getDeliveryType())) {
            if (form.getCity() == null || form.getCity().isBlank()
                    || form.getStreet() == null || form.getStreet().isBlank()
                    || form.getBuilding() == null || form.getBuilding().isBlank()) {
                ra.addFlashAttribute("error", "Заполните адрес доставки: город, улица, дом");
                return "redirect:/client/cart";
            }
        }

        BigDecimal total = items.stream()
                .map(ci -> {
                    if (!ci.getLoan().getCollaterals().isEmpty()) {
                        Collateral c = ci.getLoan().getCollaterals().get(0);
                        return c.getEstimatedPrice() != null ? c.getEstimatedPrice() : BigDecimal.ZERO;
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Account account = accountService.getOrCreateForClient(client);
        if (account.getBalance() == null || account.getBalance().compareTo(total) < 0) {
            ra.addFlashAttribute("error",
                    "Недостаточно средств. Нужно " + total.toPlainString()
                            + " ₽, на счёте " + account.getBalance().toPlainString() + " ₽");
            return "redirect:/client/cart";
        }

        try {
            accountService.purchase(client.getId(), total,
                    "Оплата заказа (" + items.size() + " тов.)");

            Order order = orderService.createOrder(client, items, form.getDeliveryType(),
                    form.getCity(), form.getStreet(), form.getBuilding(), form.getApartment(),
                    form.getPickupAddress(), form.getContactPhone(), form.getComment(), total);

            cartService.clear(client.getId());

            String deliveryText = "DELIVERY".equals(form.getDeliveryType())
                    ? "Доставка по адресу: г. " + form.getCity() + ", ул. " + form.getStreet() + ", д. " + form.getBuilding()
                    : "Самовывоз: " + (form.getPickupAddress() != null ? form.getPickupAddress() : "офис");

            StringBuilder itemNumbers = new StringBuilder();
            for (CartItem ci : items) {
                if (!ci.getLoan().getCollaterals().isEmpty()
                        && ci.getLoan().getCollaterals().get(0).getItemNumber() != null) {
                    if (itemNumbers.length() > 0) itemNumbers.append(", ");
                    itemNumbers.append(ci.getLoan().getCollaterals().get(0).getItemNumber());
                }
            }

            notificationService.notify(client, "ORDER_PLACED", "SUCCESS",
                    order.getOrderNumber(),
                    total.toPlainString(),
                    itemNumbers.toString(),
                    deliveryText);

            notificationService.notifyStaff("ORDER_PLACED", "SUCCESS",
                    order.getOrderNumber(),
                    total.toPlainString(),
                    itemNumbers.toString(),
                    client.getFullName() + " (" + client.getClientNumber() + "). " + deliveryText);

            ra.addFlashAttribute("success",
                    "Заказ " + order.getOrderNumber() + " оформлен! Сумма " +
                            total.toPlainString() + " ₽ списана со счёта.");
            return "redirect:/client/orders/" + order.getId();
        } catch (Exception e) {
            // откатываю всё: если заказ не создался, деньги со счёта списаться не должны
            TransactionUtils.rollbackCurrent();
            ra.addFlashAttribute("error", "Ошибка оформления: " + e.getMessage());
            return "redirect:/client/cart";
        }
    }
}