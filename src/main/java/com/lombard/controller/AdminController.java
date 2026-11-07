package com.lombard.controller;

import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.ClientService;
import com.lombard.service.LoanService;
import com.lombard.service.UserService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "CLIENT");

    private final UserService userService;
    private final LoanService loanService;
    private final ClientService clientService;

    public AdminController(UserService userService,
                           LoanService loanService,
                           ClientService clientService) {
        this.userService = userService;
        this.loanService = loanService;
        this.clientService = clientService;
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String query,
                        @RequestParam(required = false) String role,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        List<User> users = new ArrayList<>(userService.findAll());

        if (query != null && !query.isEmpty()) {
            String q = query.toLowerCase().trim();
            users.removeIf(u -> {
                boolean matchUsername = u.getUsername() != null
                        && u.getUsername().toLowerCase().contains(q);
                boolean matchNumber = u.getUserNumber() != null
                        && u.getUserNumber().toLowerCase().contains(q);
                return !(matchUsername || matchNumber);
            });
        }

        if (role != null && !role.isEmpty()) {
            users.removeIf(u -> u.getRole() == null || !u.getRole().equals(role));
        }

        switch (sort) {
            case "number":
                users.sort(Comparator.comparing(
                        u -> u.getUserNumber() != null ? u.getUserNumber() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            case "username":
                users.sort(Comparator.comparing(
                        u -> u.getUsername() != null ? u.getUsername().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            case "role":
                users.sort(Comparator.comparing(
                        u -> u.getRole() != null ? u.getRole() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            default:
                users.sort(Comparator.comparing(User::getId));
        }

        int totalItems = users.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<User> pageContent = totalItems > 0 ? users.subList(fromIndex, toIndex) : List.of();

        model.addAttribute("users", pageContent);
        model.addAttribute("query", query);
        model.addAttribute("role", role);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        return "admin_users";
    }

    @GetMapping("/users/export")
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<User> users = userService.findAll();
        List<String[]> rows = new ArrayList<>();

        for (User u : users) {
            rows.add(new String[]{
                    String.valueOf(u.getId()),
                    CsvExporter.esc(u.getUserNumber()),
                    CsvExporter.esc(u.getUsername()),
                    CsvExporter.esc(u.getRole())
            });
        }

        CsvExporter.write(response, "users.csv", rows,
                "ID", "Номер", "Логин", "Роль");
    }

    @PostMapping("/users/import")
    public String importUsers(@RequestParam("file") MultipartFile file,
                              RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Файл не выбран");
            return "redirect:/admin/users";
        }

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    line = line.replace("\uFEFF", "");
                    String low = line.toLowerCase();
                    if (low.startsWith("username") || low.startsWith("логин")) {
                        continue;
                    }
                }

                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 3) {
                    errors.add("Мало полей: " + line);
                    skipped++;
                    continue;
                }

                String username = parts[0].trim();
                String password = parts[1].trim();
                String role = parts[2].trim().toUpperCase();

                if (username.isEmpty() || password.isEmpty()) {
                    errors.add("Пустой логин/пароль: " + line);
                    skipped++;
                    continue;
                }

                if (password.length() < 6) {
                    errors.add("Короткий пароль для '" + username + "'");
                    skipped++;
                    continue;
                }

                if (!ALLOWED_ROLES.contains(role)) {
                    errors.add("Неизвестная роль '" + role + "'");
                    skipped++;
                    continue;
                }

                try {
                    userService.register(username, password, role);
                    created++;
                } catch (RuntimeException e) {
                    errors.add("Логин '" + username + "' уже занят");
                    skipped++;
                }
            }

        } catch (IOException e) {
            ra.addFlashAttribute("error", "Ошибка чтения файла: " + e.getMessage());
            return "redirect:/admin/users";
        }

        StringBuilder msg = new StringBuilder("Импорт завершён: создано " + created);
        if (skipped > 0) msg.append(", пропущено ").append(skipped);
        ra.addFlashAttribute("success", msg.toString());

        if (!errors.isEmpty()) {
            ra.addFlashAttribute("info", String.join("; ", errors));
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            ra.addFlashAttribute("error", "Недопустимая роль: " + role);
            return "redirect:/admin/users";
        }

        User target = userService.findById(id);
        if (target == null) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/admin/users";
        }

        // === ЗАЩИТА: нельзя менять роль самому себе ===
        if (auth.getName().equals(target.getUsername())) {
            ra.addFlashAttribute("error", "Нельзя изменить свою собственную роль");
            return "redirect:/admin/users";
        }

        userService.changeRole(id, role.toUpperCase());
        ra.addFlashAttribute("success", "Роль изменена на «" + role.toUpperCase() + "»");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin_user_form";
    }

    @PostMapping("/users/new")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             Model model,
                             RedirectAttributes ra) {
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            model.addAttribute("error", "Недопустимая роль: " + role);
            model.addAttribute("user", new User());
            return "admin_user_form";
        }
        try {
            userService.register(username, password, role.toUpperCase());
            ra.addFlashAttribute("success", "Пользователь «" + username + "» создан");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", new User());
            return "admin_user_form";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            userService.delete(id, auth.getName());
            ra.addFlashAttribute("success", "Пользователь удалён");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalClients", clientService.count());
        model.addAttribute("totalLoans", loanService.count());
        model.addAttribute("avgLoanAmount", loanService.getAverageLoanAmount());
        model.addAttribute("avgWaitingDays", loanService.getAverageWaitingDays());
        model.addAttribute("loansByStatus", loanService.countByStatus());
        model.addAttribute("amountsByStatus", loanService.sumByStatus());

        List<Loan> allLoans = loanService.findAll();

        BigDecimal totalIssued = allLoans.stream()
                .filter(l -> "Выдан".equals(l.getStatus()) || "Просрочен".equals(l.getStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalClosed = allLoans.stream()
                .filter(l -> "Закрыт".equals(l.getStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSold = allLoans.stream()
                .filter(l -> "На продаже".equals(l.getStatus()) || "Куплен".equals(l.getStatus()))
                .map(l -> {
                    if (!l.getCollaterals().isEmpty() && l.getCollaterals().get(0).getEstimatedPrice() != null) {
                        return l.getCollaterals().get(0).getEstimatedPrice();
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = allLoans.stream()
                .filter(l -> "Просрочен".equals(l.getStatus()) || "На складе".equals(l.getStatus()))
                .count();

        double overduePercent = allLoans.isEmpty() ? 0.0 : (double) overdueCount / allLoans.size() * 100;

        double avgTermMonths = allLoans.stream()
                .filter(l -> l.getCreatedDate() != null && l.getDueDate() != null)
                .mapToLong(l -> ChronoUnit.DAYS.between(l.getCreatedDate(), l.getDueDate()))
                .average()
                .orElse(0.0) / 30.44;

        model.addAttribute("totalIssued", totalIssued);
        model.addAttribute("totalClosed", totalClosed);
        model.addAttribute("totalSold", totalSold);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("overduePercent", overduePercent);
        model.addAttribute("avgTermMonths", avgTermMonths);

        return "admin_stats";
    }
}