package com.lombard.controller;

import com.lombard.model.Loan;
import com.lombard.service.LoanService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/intake")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class IntakeController {

    private static final int PAGE_SIZE = 10;
    private final LoanService loanService;

    public IntakeController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public String intake(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(required = false) String query,
                         @RequestParam(defaultValue = "new") String sort,
                         Model model, Authentication auth) {

        List<Loan> all = new ArrayList<>(loanService.findAll());
        all.removeIf(l -> !"В ожидании".equals(l.getStatus()));

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase().trim();
            all.removeIf(l -> {
                boolean matchLoan = l.getLoanNumber() != null && l.getLoanNumber().toLowerCase().contains(q);
                boolean matchClient = l.getClient() != null && l.getClient().getFullName() != null && l.getClient().getFullName().toLowerCase().contains(q);
                boolean matchItem = !l.getCollaterals().isEmpty() && l.getCollaterals().get(0).getName() != null && l.getCollaterals().get(0).getName().toLowerCase().contains(q);
                boolean matchItemNumber = !l.getCollaterals().isEmpty() && l.getCollaterals().get(0).getItemNumber() != null && l.getCollaterals().get(0).getItemNumber().toLowerCase().contains(q);
                return !(matchLoan || matchClient || matchItem || matchItemNumber);
            });
        }

        switch (sort) {
            case "old":
                all.sort(Comparator.comparing(Loan::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "amount_desc":
                all.sort(Comparator.comparing(Loan::getAmount, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "amount_asc":
                all.sort(Comparator.comparing(Loan::getAmount, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "new":
            default:
                all.sort(Comparator.comparing(Loan::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
        }

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
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        return "intake";
    }

    // НОВЫЙ МЕТОД ДЛЯ ЭКСПОРТА В CSV
    @GetMapping("/export")
    public void exportIntake(HttpServletResponse response,
                             Authentication auth) throws IOException {

        List<Loan> all = new ArrayList<>(loanService.findAll());
        all.removeIf(l -> !"В ожидании".equals(l.getStatus()));

        List<String[]> rows = new ArrayList<>();
        for (Loan l : all) {
            String itemName = "";
            String itemDesc = "";
            if (!l.getCollaterals().isEmpty()) {
                itemName = l.getCollaterals().get(0).getName() != null ? l.getCollaterals().get(0).getName() : "";
                itemDesc = l.getCollaterals().get(0).getDescription() != null ? l.getCollaterals().get(0).getDescription() : "";
            }

            rows.add(new String[]{
                    l.getLoanNumber(),
                    l.getClient() != null ? CsvExporter.esc(l.getClient().getFullName()) : "",
                    CsvExporter.esc(itemName),
                    CsvExporter.esc(itemDesc),
                    l.getAmount() != null ? l.getAmount().toPlainString() : "",
                    l.getCreatedDate() != null ? l.getCreatedDate().toString() : ""
            });
        }

        CsvExporter.write(response, "intake_requests.csv", rows,
                "Номер заявки", "Клиент", "Вещь", "Описание", "Запрошено", "Дата");
    }
}