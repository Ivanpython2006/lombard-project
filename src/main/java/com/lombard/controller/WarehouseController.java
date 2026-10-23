package com.lombard.controller;

import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.service.CollateralService;
import com.lombard.service.FileStorageService;
import com.lombard.service.LoanService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/warehouse")
public class WarehouseController {

    private static final int PAGE_SIZE = 10;

    private final LoanService loanService;
    private final FileStorageService fileStorageService;
    private final CollateralService collateralService;

    public WarehouseController(LoanService loanService,
                               FileStorageService fileStorageService,
                               CollateralService collateralService) {
        this.loanService = loanService;
        this.fileStorageService = fileStorageService;
        this.collateralService = collateralService;
    }

    private boolean isAdminOrManager(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String warehouse(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "id") String sort,
                            @RequestParam(required = false) String query,
                            Model model, Authentication auth) {
        if (!isAdminOrManager(auth)) return "redirect:/";

        List<Loan> all = new ArrayList<>(loanService.findAll());
        all.removeIf(l -> !"На складе".equals(l.getStatus()));

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
            case "number":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getItemNumber() != null
                                ? l.getCollaterals().get(0).getItemNumber() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "item":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getName() != null
                                ? l.getCollaterals().get(0).getName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "price":
                cmp = Comparator.comparing(
                        l -> !l.getCollaterals().isEmpty()
                                && l.getCollaterals().get(0).getEstimatedPrice() != null
                                ? l.getCollaterals().get(0).getEstimatedPrice() : BigDecimal.ZERO,
                        Comparator.nullsLast(BigDecimal::compareTo));
                break;
            case "client":
                cmp = Comparator.comparing(
                        l -> l.getClient() != null && l.getClient().getFullName() != null
                                ? l.getClient().getFullName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            default:
                cmp = Comparator.comparing(Loan::getId).reversed();
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

        model.addAttribute("loans", pageContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("sort", sort);
        model.addAttribute("query", query);
        model.addAttribute("isAdmin", isAdmin(auth));
        return "warehouse";
    }

    @PostMapping("/{id}/save")
    public String saveItem(@PathVariable Long id,
                           @RequestParam(required = false) String itemName,
                           @RequestParam(required = false) String itemDescription,
                           @RequestParam(required = false) BigDecimal price,
                           @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                           Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/";
        Loan loan = loanService.findById(id);
        if (loan == null || !"На складе".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя редактировать этот товар");
            return "redirect:/admin/warehouse";
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
            if (photos != null && photos.length > 0) {
                try {
                    collateralService.appendPhotos(c, photos, "item_" + c.getId());
                } catch (RuntimeException e) {
                    ra.addFlashAttribute("error", e.getMessage());
                    return "redirect:/admin/warehouse";
                }
            }
        }
        loanService.save(loan);
        ra.addFlashAttribute("success", "Товар обновлён");
        return "redirect:/admin/warehouse";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id,
                          @RequestParam(required = false) BigDecimal price,
                          @RequestParam(required = false) String itemName,
                          @RequestParam(required = false) String itemDescription,
                          Authentication auth,
                          RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/";
        Loan loan = loanService.findById(id);
        if (loan == null || !"На складе".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя вернуть этот товар на витрину");
            return "redirect:/admin/warehouse";
        }

        if (!loan.getCollaterals().isEmpty()) {
            Collateral c = loan.getCollaterals().get(0);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) c.setEstimatedPrice(price);
            if (itemName != null && !itemName.isBlank()) c.setName(itemName.trim());
            if (itemDescription != null && !itemDescription.isBlank()) c.setDescription(itemDescription.trim());
        }

        loan.setStatus("На продаже");
        loanService.save(loan);
        ra.addFlashAttribute("success", "Товар выложен в магазин");
        return "redirect:/admin/warehouse";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication auth,
                         RedirectAttributes ra) {
        if (!isAdmin(auth)) {
            ra.addFlashAttribute("error", "Удаление доступно только администратору");
            return "redirect:/admin/warehouse";
        }
        Loan loan = loanService.findById(id);
        if (loan == null || !"На складе".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя удалить этот товар");
            return "redirect:/admin/warehouse";
        }
        String loanNumber = loan.getLoanNumber();
        loanService.delete(id);
        ra.addFlashAttribute("success", "Залог " + loanNumber + " удалён");
        return "redirect:/admin/warehouse";
    }

    @GetMapping("/export")
    public void exportWarehouse(HttpServletResponse response,
                                Authentication auth) throws IOException {
        if (!isAdminOrManager(auth)) {
            response.sendRedirect("/403");
            return;
        }

        List<Loan> loans = loanService.findAll().stream()
                .filter(l -> "На складе".equals(l.getStatus()))
                .collect(Collectors.toList());

        List<String[]> rows = new ArrayList<>();
        for (Loan l : loans) {
            String name = "";
            String price = "";
            String number = "";
            if (!l.getCollaterals().isEmpty()) {
                Collateral c = l.getCollaterals().get(0);
                name = c.getName() != null ? c.getName() : "";
                price = c.getEstimatedPrice() != null ? c.getEstimatedPrice().toPlainString() : "";
                number = c.getItemNumber() != null ? c.getItemNumber() : "";
            }
            rows.add(new String[]{
                    String.valueOf(l.getId()),
                    CsvExporter.esc(number),
                    l.getClient() != null ? CsvExporter.esc(l.getClient().getFullName()) : "",
                    CsvExporter.esc(name),
                    price,
                    l.getStatus() != null ? CsvExporter.esc(l.getStatus()) : ""
            });
        }

        CsvExporter.write(response, "warehouse.csv", rows,
                "ID займа", "Номер", "Клиент", "Вещь", "Оценка", "Статус");
    }
}