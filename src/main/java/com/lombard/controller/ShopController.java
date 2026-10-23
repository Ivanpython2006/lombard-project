package com.lombard.controller;

import com.lombard.model.CartItem;
import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.CartService;
import com.lombard.service.ClientService;
import com.lombard.service.LoanService;
import com.lombard.service.UserService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private static final int PAGE_SIZE = 10;

    private final LoanService loanService;
    private final CartService cartService;
    private final ClientService clientService;
    private final UserService userService;

    public ShopController(LoanService loanService,
                          CartService cartService,
                          ClientService clientService,
                          UserService userService) {
        this.loanService = loanService;
        this.cartService = cartService;
        this.clientService = clientService;
        this.userService = userService;
    }

    private boolean isAdminOrManager(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    private boolean isClient(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
    }

    private Client getCurrentClient(Authentication auth) {
        if (auth == null) return null;
        User user = userService.findByUsername(auth.getName());
        if (user == null) return null;
        return clientService.findByUserId(user.getId());
    }

    @GetMapping
    public String shop(@RequestParam(defaultValue = "1") int page,
                       Model model, Authentication auth) {
        List<Loan> onSale = loanService.findAll().stream()
                .filter(l -> "На продаже".equals(l.getStatus()))
                .collect(Collectors.toList());

        int totalItems = onSale.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<Loan> pageContent = totalItems > 0 ? onSale.subList(fromIndex, toIndex) : List.of();

        Client client = getCurrentClient(auth);
        List<Long> cartLoanIds = new ArrayList<>();
        long cartCount = 0;
        if (client != null) {
            List<CartItem> items = cartService.getByClient(client.getId());
            for (CartItem ci : items) cartLoanIds.add(ci.getLoan().getId());
            cartCount = items.size();
        }

        model.addAttribute("loans", pageContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("isAdminOrManager", isAdminOrManager(auth));
        model.addAttribute("isClient", isClient(auth));
        model.addAttribute("cartLoanIds", cartLoanIds);
        model.addAttribute("cartCount", cartCount);
        return "shop";
    }

    @GetMapping("/export")
    public void exportShop(HttpServletResponse response, Authentication auth) throws IOException {
        if (!isAdminOrManager(auth)) {
            response.sendRedirect("/403");
            return;
        }

        List<Loan> onSale = loanService.findAll().stream()
                .filter(l -> "На продаже".equals(l.getStatus()))
                .collect(Collectors.toList());

        List<String[]> rows = new ArrayList<>();
        for (Loan l : onSale) {
            String name = "", desc = "", price = "", number = "";
            if (!l.getCollaterals().isEmpty()) {
                Collateral c = l.getCollaterals().get(0);
                name = c.getName() != null ? c.getName() : "";
                desc = c.getDescription() != null ? c.getDescription() : "";
                price = c.getEstimatedPrice() != null ? c.getEstimatedPrice().toPlainString() : "";
                number = c.getItemNumber() != null ? c.getItemNumber() : "";
            }
            rows.add(new String[]{String.valueOf(l.getId()), CsvExporter.esc(number),
                    CsvExporter.esc(name), CsvExporter.esc(desc), price});
        }
        CsvExporter.write(response, "shop.csv", rows,
                "ID займа", "Номер", "Вещь", "Описание", "Цена");
    }

    @PostMapping("/{id}/add-to-cart")
    public String addToCart(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Client client = getCurrentClient(auth);
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/shop";
        }
        Loan loan = loanService.findById(id);
        if (loan == null || !"На продаже".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Товар недоступен");
            return "redirect:/shop";
        }
        try {
            cartService.add(client, loan);
            ra.addFlashAttribute("success", "Товар добавлен в корзину");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shop";
    }

    @GetMapping("/manage")
    public String manage(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "id") String sort,
                         @RequestParam(required = false) String query,
                         Model model, Authentication auth) {
        if (!isAdminOrManager(auth)) return "redirect:/shop";

        List<Loan> all = new ArrayList<>(loanService.findAll());
        String filterStatus = (status == null || status.isEmpty()) ? "На продаже" : status;
        if (!"all".equals(filterStatus)) {
            all.removeIf(l -> !filterStatus.equals(l.getStatus()));
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase().trim();
            all.removeIf(l -> {
                boolean matchNumber = !l.getCollaterals().isEmpty()
                        && l.getCollaterals().get(0).getItemNumber() != null
                        && l.getCollaterals().get(0).getItemNumber().toLowerCase().contains(q);
                boolean matchItem = !l.getCollaterals().isEmpty()
                        && l.getCollaterals().get(0).getName() != null
                        && l.getCollaterals().get(0).getName().toLowerCase().contains(q);
                boolean matchClient = l.getClient() != null
                        && l.getClient().getFullName() != null
                        && l.getClient().getFullName().toLowerCase().contains(q);
                boolean matchLoan = l.getLoanNumber() != null
                        && l.getLoanNumber().toLowerCase().contains(q);
                return !(matchNumber || matchItem || matchClient || matchLoan);
            });
        }

        Comparator<Loan> cmp;
        switch (sort) {
            case "date_asc":
                cmp = Comparator.comparing(
                        l -> l.getCreatedDate() != null ? l.getCreatedDate() : LocalDate.MIN,
                        Comparator.nullsLast(LocalDate::compareTo));
                break;
            case "price_desc":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getEstimatedPrice() != null
                                ? l.getCollaterals().get(0).getEstimatedPrice() : BigDecimal.ZERO,
                        Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "price_asc":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getEstimatedPrice() != null
                                ? l.getCollaterals().get(0).getEstimatedPrice() : BigDecimal.ZERO,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "item":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getName() != null
                                ? l.getCollaterals().get(0).getName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "id":
            default:
                cmp = Comparator.comparing(Loan::getId).reversed();
                break;
        }
        all.sort(cmp);

        int totalItems = all.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<Loan> pageContent = totalItems > 0 ? all.subList(fromIndex, toIndex) : List.of();

        List<Loan> allLoans = loanService.findAll();
        long countOnSale = allLoans.stream().filter(l -> "На продаже".equals(l.getStatus())).count();
        long countInStock = allLoans.stream().filter(l -> "На складе".equals(l.getStatus())).count();
        long countIssued = allLoans.stream().filter(l -> "Выдан".equals(l.getStatus())).count();

        model.addAttribute("loans", pageContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("filterStatus", filterStatus);
        model.addAttribute("sort", sort);
        model.addAttribute("query", query);
        model.addAttribute("countOnSale", countOnSale);
        model.addAttribute("countInStock", countInStock);
        model.addAttribute("countIssued", countIssued);
        return "shop_manage";
    }

    @PostMapping("/{id}/remove")
    public String removeFromSale(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/shop";
        Loan loan = loanService.findById(id);
        if (loan == null || !"На продаже".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя снять этот товар");
            return "redirect:/shop/manage";
        }
        loan.setStatus("На складе");
        loanService.save(loan);
        ra.addFlashAttribute("success", "Товар снят с витрины — на складе");
        return "redirect:/shop/manage";
    }

    @PostMapping("/{id}/save")
    public String saveItem(@PathVariable Long id,
                           @RequestParam(required = false) String itemName,
                           @RequestParam(required = false) String itemDescription,
                           @RequestParam(required = false) BigDecimal price,
                           Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/shop";
        Loan loan = loanService.findById(id);
        if (loan == null || !"На складе".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя редактировать этот товар");
            return "redirect:/shop/manage";
        }

        if (!loan.getCollaterals().isEmpty()) {
            Collateral c = loan.getCollaterals().get(0);
            if (itemName != null && !itemName.isBlank()) {
                c.setName(itemName.trim());
            }
            if (itemDescription != null) {
                c.setDescription(itemDescription.trim());
            }
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                c.setEstimatedPrice(price);
            }
        }
        loanService.save(loan);
        ra.addFlashAttribute("success", "Товар обновлён");
        return "redirect:/shop/manage";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id,
                          @RequestParam(required = false) BigDecimal price,
                          @RequestParam(required = false) String itemName,
                          @RequestParam(required = false) String itemDescription,
                          Authentication auth,
                          RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/shop";
        Loan loan = loanService.findById(id);
        if (loan == null || !"На складе".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя вернуть этот товар");
            return "redirect:/shop/manage";
        }

        if (!loan.getCollaterals().isEmpty()) {
            Collateral c = loan.getCollaterals().get(0);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                c.setEstimatedPrice(price);
            }
            if (itemName != null && !itemName.isBlank()) {
                c.setName(itemName.trim());
            }
            if (itemDescription != null && !itemDescription.isBlank()) {
                c.setDescription(itemDescription.trim());
            }
        }

        loan.setStatus("На продаже");
        loanService.save(loan);
        ra.addFlashAttribute("success", "Товар возвращён в магазин");
        return "redirect:/shop/manage";
    }
}