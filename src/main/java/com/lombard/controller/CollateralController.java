package com.lombard.controller;

import com.lombard.model.Collateral;
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

@Controller
@RequestMapping("/collaterals")
public class CollateralController {

    private static final int PAGE_SIZE = 10;

    private final CollateralService collateralService;
    private final LoanService loanService;
    private final FileStorageService fileStorageService;

    public CollateralController(CollateralService collateralService,
                                LoanService loanService,
                                FileStorageService fileStorageService) {
        this.collateralService = collateralService;
        this.loanService = loanService;
        this.fileStorageService = fileStorageService;
    }

    private boolean isAdminOrManager(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    @GetMapping
    public String list(@RequestParam(required = false) String query,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "id") String sort,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {

        List<Collateral> collaterals;
        if (query != null && !query.isEmpty()) {
            collaterals = collateralService.searchByAnyField(query);
        } else {
            collaterals = collateralService.findAll();
        }
        collaterals = new ArrayList<>(collaterals);

        if (status != null && !status.isEmpty()) {
            collaterals.removeIf(c -> c.getLoan() == null
                    || !status.equals(c.getLoan().getStatus()));
        }

        Comparator<Collateral> cmp;
        switch (sort) {
            case "number":
                cmp = Comparator.comparing(
                        c -> c.getItemNumber() != null ? c.getItemNumber() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "name":
                cmp = Comparator.comparing(
                        c -> c.getName() != null ? c.getName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "price":
                cmp = Comparator.comparing(
                        c -> c.getEstimatedPrice() != null ? c.getEstimatedPrice() : BigDecimal.ZERO,
                        Comparator.nullsLast(BigDecimal::compareTo));
                break;
            case "client":
                cmp = Comparator.comparing(
                        c -> (c.getLoan() != null && c.getLoan().getClient() != null
                                && c.getLoan().getClient().getFullName() != null)
                                ? c.getLoan().getClient().getFullName() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "status":
                cmp = Comparator.comparing(
                        c -> (c.getLoan() != null && c.getLoan().getStatus() != null)
                                ? c.getLoan().getStatus() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            default:
                cmp = Comparator.comparing(Collateral::getId);
        }
        collaterals.sort(cmp);

        int totalItems = collaterals.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<Collateral> pageContent = totalItems > 0 ? collaterals.subList(fromIndex, toIndex) : List.of();

        model.addAttribute("collaterals", pageContent);
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        return "collaterals";
    }

    @GetMapping("/export")
    public void exportCollaterals(HttpServletResponse response) throws IOException {
        List<Collateral> collaterals = collateralService.findAll();
        List<String[]> rows = new ArrayList<>();

        for (Collateral c : collaterals) {
            String loanId = c.getLoan() != null ? String.valueOf(c.getLoan().getId()) : "";
            String clientName = (c.getLoan() != null && c.getLoan().getClient() != null)
                    ? CsvExporter.esc(c.getLoan().getClient().getFullName()) : "";
            String status = c.getLoan() != null && c.getLoan().getStatus() != null
                    ? CsvExporter.esc(c.getLoan().getStatus()) : "";

            rows.add(new String[]{
                    String.valueOf(c.getId()),
                    CsvExporter.esc(c.getItemNumber()),
                    CsvExporter.esc(c.getName()),
                    CsvExporter.esc(c.getDescription()),
                    c.getEstimatedPrice() != null ? c.getEstimatedPrice().toPlainString() : "",
                    loanId,
                    clientName,
                    status
            });
        }

        CsvExporter.write(response, "collaterals.csv", rows,
                "ID", "Номер", "Вещь", "Описание", "Оценка", "ID займа", "Клиент", "Статус займа");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/collaterals";
        Collateral collateral = collateralService.findById(id);
        if (collateral == null) {
            ra.addFlashAttribute("error", "Залог не найден");
            return "redirect:/collaterals";
        }
        model.addAttribute("collateral", collateral);
        return "collateral_edit";
    }

    @PostMapping("/{id}/edit")
    public String editSave(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) BigDecimal estimatedPrice,
                           @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                           Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/collaterals";
        Collateral collateral = collateralService.findById(id);
        if (collateral == null) {
            ra.addFlashAttribute("error", "Залог не найден");
            return "redirect:/collaterals";
        }

        if (name == null || name.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Название обязательно");
            return "redirect:/collaterals/" + id + "/edit";
        }
        if (estimatedPrice != null && estimatedPrice.compareTo(BigDecimal.ZERO) < 0) {
            ra.addFlashAttribute("error", "Оценка не может быть отрицательной");
            return "redirect:/collaterals/" + id + "/edit";
        }

        if (photos != null && photos.length > 0) {
            try {
                collateralService.appendPhotos(collateral, photos, "item_" + id);
            } catch (RuntimeException e) {
                ra.addFlashAttribute("error", e.getMessage());
                return "redirect:/collaterals/" + id + "/edit";
            }
        }

        collateral.setName(name.trim());
        collateral.setDescription(description != null ? description.trim() : null);
        collateral.setEstimatedPrice(estimatedPrice);
        collateralService.save(collateral);

        ra.addFlashAttribute("success", "Залог обновлён");
        return "redirect:/collaterals";
    }

    @PostMapping("/{id}/photo/delete")
    public String deletePhoto(@PathVariable Long id,
                              @RequestParam String photoUrl,
                              @RequestParam(required = false) String returnTo,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/collaterals";
        Collateral collateral = collateralService.findById(id);
        if (collateral == null) {
            ra.addFlashAttribute("error", "Залог не найден");
            return "redirect:/collaterals";
        }

        Long loanId = collateral.getLoan() != null ? collateral.getLoan().getId() : null;

        collateralService.removePhoto(collateral, photoUrl);
        ra.addFlashAttribute("success", "Фото удалено");

        if (returnTo != null && returnTo.startsWith("/loans/") && loanId != null) {
            return "redirect:/loans/" + loanId + "/edit";
        }
        return "redirect:/collaterals/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/collaterals";
        Collateral collateral = collateralService.findById(id);
        if (collateral == null) {
            ra.addFlashAttribute("error", "Залог не найден");
            return "redirect:/collaterals";
        }
        collateralService.delete(id);
        ra.addFlashAttribute("success", "Залог удалён");
        return "redirect:/collaterals";
    }
}