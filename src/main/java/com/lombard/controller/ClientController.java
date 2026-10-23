package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.AccountService;
import com.lombard.service.ClientService;
import com.lombard.service.CollateralService;
import com.lombard.service.FileStorageService;
import com.lombard.service.LoanService;
import com.lombard.service.NotificationService;
import com.lombard.service.UserService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/client")
public class ClientController {

    private static final int PAGE_SIZE = 10;

    private final ClientService clientService;
    private final LoanService loanService;
    private final UserService userService;
    private final CollateralService collateralService;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    public ClientController(ClientService clientService,
                            LoanService loanService,
                            UserService userService,
                            CollateralService collateralService,
                            AccountService accountService,
                            NotificationService notificationService,
                            FileStorageService fileStorageService) {
        this.clientService = clientService;
        this.loanService = loanService;
        this.userService = userService;
        this.collateralService = collateralService;
        this.accountService = accountService;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/search")
    public String clients(@RequestParam(required = false) String query,
                          @RequestParam(defaultValue = "id") String sort,
                          @RequestParam(defaultValue = "1") int page,
                          Model model) {
        List<Client> clients;
        if (query != null && !query.isEmpty()) {
            clients = clientService.searchByAnyField(query);
        } else {
            clients = clientService.findAll();
        }
        clients = new ArrayList<>(clients);
        switch (sort) {
            case "name":
                clients.sort(Comparator.comparing(
                        c -> c.getFullName() != null ? c.getFullName().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            case "email":
                clients.sort(Comparator.comparing(
                        c -> c.getEmail() != null ? c.getEmail().toLowerCase() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            case "phone":
                clients.sort(Comparator.comparing(
                        c -> c.getPhone() != null ? c.getPhone() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            default:
                clients.sort(Comparator.comparing(Client::getId));
        }

        int totalItems = clients.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<Client> pageContent = totalItems > 0 ? clients.subList(fromIndex, toIndex) : List.of();

        model.addAttribute("clients", pageContent);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        return "clients";
    }

    @GetMapping("/export")
    public void exportClients(HttpServletResponse response) throws IOException {
        List<Client> clients = clientService.findAll();
        List<String[]> rows = new ArrayList<>();

        for (Client c : clients) {
            rows.add(new String[]{
                    String.valueOf(c.getId()),
                    CsvExporter.esc(c.getClientNumber()),
                    CsvExporter.esc(c.getFullName()),
                    CsvExporter.esc(c.getEmail()),
                    CsvExporter.esc(c.getPhone())
            });
        }

        CsvExporter.write(response, "clients.csv", rows,
                "ID", "Номер", "ФИО", "Email", "Телефон");
    }

    @PostMapping("/{id}/delete")
    public String deleteClient(@PathVariable Long id, RedirectAttributes ra) {
        Client client = clientService.findById(id);
        if (client == null) {
            ra.addFlashAttribute("error", "Клиент не найден");
            return "redirect:/client/search";
        }
        try {
            clientService.deleteClient(id);
            ra.addFlashAttribute("success", "Клиент удалён");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/client/search";
    }

    @GetMapping("/loans")
    public String myLoans(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "id") String sort,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String query,
                          Model model, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        List<Loan> loans = new ArrayList<>(loanService.findByClientId(client.getId()));

        if (status != null && !status.isEmpty()) {
            loans.removeIf(l -> !status.equals(l.getStatus()));
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase().trim();
            loans.removeIf(l -> {
                boolean matchLoanNumber = l.getLoanNumber() != null
                        && l.getLoanNumber().toLowerCase().contains(q);
                boolean matchItemName = !l.getCollaterals().isEmpty()
                        && l.getCollaterals().get(0).getName() != null
                        && l.getCollaterals().get(0).getName().toLowerCase().contains(q);
                boolean matchItemNumber = !l.getCollaterals().isEmpty()
                        && l.getCollaterals().get(0).getItemNumber() != null
                        && l.getCollaterals().get(0).getItemNumber().toLowerCase().contains(q);
                return !(matchLoanNumber || matchItemName || matchItemNumber);
            });
        }

        switch (sort) {
            case "amount":
                loans.sort(Comparator.comparing(
                        l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO,
                        Comparator.reverseOrder()));
                break;
            case "due":
                loans.sort(Comparator.comparing(
                        l -> l.getDueDate() != null ? l.getDueDate() : LocalDate.MAX,
                        Comparator.nullsLast(LocalDate::compareTo)));
                break;
            case "status":
                loans.sort(Comparator.comparing(
                        l -> l.getStatus() != null ? l.getStatus() : "",
                        Comparator.nullsLast(String::compareTo)));
                break;
            default:
                loans.sort(Comparator.comparing(Loan::getId).reversed());
        }

        int totalItems = loans.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<Loan> pageContent = totalItems > 0 ? loans.subList(fromIndex, toIndex) : List.of();

        model.addAttribute("loans", pageContent);
        model.addAttribute("sort", sort);
        model.addAttribute("status", status);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        return "client_loans";
    }

    @GetMapping("/loans/export")
    public void exportMyLoans(HttpServletResponse response,
                              Authentication auth) throws IOException {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());

        List<Loan> loans = (client == null)
                ? List.of()
                : new ArrayList<>(loanService.findByClientId(client.getId()));

        List<String[]> rows = new ArrayList<>();
        for (Loan l : loans) {
            String name = "";
            String itemNumber = "";
            if (!l.getCollaterals().isEmpty()) {
                name = l.getCollaterals().get(0).getName() != null
                        ? l.getCollaterals().get(0).getName() : "";
                itemNumber = l.getCollaterals().get(0).getItemNumber() != null
                        ? l.getCollaterals().get(0).getItemNumber() : "";
            }
            rows.add(new String[]{
                    CsvExporter.esc(l.getLoanNumber()),
                    CsvExporter.esc(itemNumber),
                    CsvExporter.esc(name),
                    l.getAmount() != null ? l.getAmount().toPlainString() : "",
                    l.getRate() != null ? l.getRate().toPlainString() : "",
                    l.getRate() != null ? l.getTotalToReturn().toPlainString() : "",
                    l.getStatus() != null ? CsvExporter.esc(l.getStatus()) : "",
                    l.getCreatedDate() != null ? l.getCreatedDate().toString() : "",
                    l.getDueDate() != null ? l.getDueDate().toString() : ""
            });
        }

        CsvExporter.write(response, "my_loans.csv", rows,
                "Номер займа", "Номер вещи", "Вещь", "Сумма", "Ставка %",
                "К возврату", "Статус", "Создан", "Вернуть до");
    }

    @GetMapping("/loans/new")
    public String newLoanForm(Model model) {
        model.addAttribute("loan", new Loan());
        return "add_loan";
    }

    @PostMapping("/loans/new")
    public String createLoan(@Valid @ModelAttribute Loan loan,
                             BindingResult result,
                             @RequestParam String collateralName,
                             @RequestParam String collateralDescription,
                             @RequestParam(defaultValue = "1") int months,
                             @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (result.hasErrors()) return "add_loan";

        if (months < 1 || months > 24) {
            ra.addFlashAttribute("error", "Срок займа должен быть от 1 до 24 месяцев");
            return "redirect:/client/loans/new";
        }

        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/client/loans";
        }

        loan.setRate(null);
        loan.setClient(client);
        loan.setStatus("В ожидании");
        loan.setCreatedDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusMonths(months));
        loanService.save(loan);

        Collateral collateral = new Collateral();
        collateral.setName(collateralName);
        collateral.setDescription(collateralDescription);
        collateral.setEstimatedPrice(null);
        collateral.setLoan(loan);
        collateralService.save(collateral);

        if (photos != null && photos.length > 0) {
            try {
                collateralService.appendPhotos(collateral, photos, "item_" + collateral.getId());
            } catch (RuntimeException e) {
                ra.addFlashAttribute("error", e.getMessage());
                return "redirect:/client/loans/new";
            }
        }

        notificationService.notify(client, "LOAN_CREATED", "INFO",
                loan.getLoanNumber(),
                collateralName,
                loan.getAmount() != null ? loan.getAmount().toPlainString() : "0"
        );

        notificationService.notifyStaff("LOAN_CREATED_STAFF", "INFO",
                loan.getLoanNumber(),
                client.getFullName(),
                client.getClientNumber(),
                collateralName,
                loan.getAmount() != null ? loan.getAmount().toPlainString() : "0"
        );

        ra.addFlashAttribute("success", "Заявка отправлена. Ожидайте оценки.");
        return "redirect:/client/loans";
    }

    @PostMapping("/loans/{id}/confirm")
    public String confirmLoan(@PathVariable Long id,
                              @RequestParam Boolean agree,
                              Authentication auth,
                              RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/client/loans";
        }

        Loan loan = loanService.findById(id);
        if (loan == null || loan.getClient() == null
                || !loan.getClient().getId().equals(client.getId())) {
            ra.addFlashAttribute("error", "Заём не найден");
            return "redirect:/client/loans";
        }
        if (!"Ожидает подтверждения".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Заявка не находится в ожидании подтверждения");
            return "redirect:/client/loans";
        }

        String itemName = loanService.getItemName(loan);

        if (Boolean.TRUE.equals(agree)) {
            loan.setStatus("Выдан");
            loan.setClientApproved(true);
            loan.setDecisionDate(LocalDate.now());
            loanService.save(loan);

            notificationService.notify(client, "LOAN_AGREED", "SUCCESS",
                    loan.getLoanNumber(),
                    itemName,
                    "Выдан"
            );

            notificationService.notifyStaff("LOAN_AGREED_STAFF", "SUCCESS",
                    client.getFullName(),
                    client.getClientNumber(),
                    loan.getLoanNumber(),
                    itemName,
                    loan.getAgreedAmount() != null ? loan.getAgreedAmount().toPlainString() : "0"
            );

            ra.addFlashAttribute("success", "Вы согласились с условиями. Заём одобрен!");
        } else {
            loan.setStatus("Отклонен");
            loan.setClientApproved(false);
            loanService.save(loan);

            notificationService.notify(client, "LOAN_DECLINED", "WARNING",
                    loan.getLoanNumber(),
                    itemName
            );

            notificationService.notifyStaff("LOAN_DECLINED_STAFF", "WARNING",
                    client.getFullName(),
                    client.getClientNumber(),
                    loan.getLoanNumber(),
                    itemName
            );

            ra.addFlashAttribute("success", "Вы отказались. Заявка отклонена.");
        }
        return "redirect:/client/loans";
    }

    @PostMapping("/loans/{id}/cancel")
    public String cancelLoan(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/client/loans";
        }

        Loan loan = loanService.findById(id);
        if (loan == null || loan.getClient() == null
                || !loan.getClient().getId().equals(client.getId())) {
            ra.addFlashAttribute("error", "Заявка не найдена");
            return "redirect:/client/loans";
        }
        if (!"В ожидании".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя отменить заявку в статусе «" + loan.getStatus() + "»");
            return "redirect:/client/loans";
        }
        loan.setStatus("Отменена");
        loanService.save(loan);

        String itemName = loanService.getItemName(loan);
        notificationService.notify(client, "LOAN_CANCELLED", "WARNING",
                loan.getLoanNumber(),
                itemName
        );

        ra.addFlashAttribute("success", "Заявка отменена");
        return "redirect:/client/loans";
    }

    @PostMapping("/loans/{id}/repay")
    public String repayLoan(@PathVariable Long id,
                            Authentication auth,
                            RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/client/loans";
        }

        Loan loan = loanService.findById(id);
        if (loan == null || loan.getClient() == null
                || !loan.getClient().getId().equals(client.getId())) {
            ra.addFlashAttribute("error", "Заём не найден");
            return "redirect:/client/loans";
        }

        if (!"Выдан".equals(loan.getStatus()) && !"Просрочен".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя погасить заём в статусе «" + loan.getStatus() + "»");
            return "redirect:/client/loans";
        }

        BigDecimal toReturn = loan.getTotalToReturn();
        String itemName = loanService.getItemName(loan);

        try {
            accountService.withdraw(client.getId(), toReturn, "Погашение займа " + loan.getLoanNumber());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error",
                    "Недостаточно средств. Нужно " + toReturn.toPlainString() + " ₽. Пополните счёт.");
            return "redirect:/client/loans";
        }

        loan.setStatus("Закрыт");
        loanService.save(loan);

        notificationService.notify(client, "LOAN_REPAID", "SUCCESS",
                loan.getLoanNumber(),
                itemName,
                toReturn.toPlainString()
        );

        notificationService.notifyStaff("LOAN_REPAID_STAFF", "SUCCESS",
                client.getFullName(),
                client.getClientNumber(),
                loan.getLoanNumber(),
                itemName,
                toReturn.toPlainString()
        );

        ra.addFlashAttribute("success",
                "Заём " + loan.getLoanNumber() + " закрыт! Списано " + toReturn.toPlainString() + " ₽");
        return "redirect:/client/loans";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth, RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }
        model.addAttribute("client", client);
        model.addAttribute("user", user);
        return "client_profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String lastName,
                                @RequestParam String firstName,
                                @RequestParam(required = false) String middleName,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Authentication auth,
                                RedirectAttributes ra) {
        User user = userService.findByUsername(auth.getName());
        Client client = clientService.findByUserId(user.getId());
        if (client == null) {
            ra.addFlashAttribute("error", "Профиль клиента не найден");
            return "redirect:/";
        }

        String nameRegex = "^[А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+$";
        String lName = lastName.trim();
        String fName = firstName.trim();
        String mName = middleName != null ? middleName.trim() : "";

        if (!lName.matches(nameRegex)) {
            ra.addFlashAttribute("error", "Фамилия должна содержать только буквы (минимум 2)");
            return "redirect:/client/profile";
        }
        if (!fName.matches(nameRegex)) {
            ra.addFlashAttribute("error", "Имя должно содержать только буквы (минимум 2)");
            return "redirect:/client/profile";
        }
        if (!mName.isEmpty() && !mName.matches(nameRegex)) {
            ra.addFlashAttribute("error", "Отчество должно содержать только буквы (минимум 2)");
            return "redirect:/client/profile";
        }

        if (!phone.matches("^\\+7\\d{10}$")) {
            ra.addFlashAttribute("error", "Телефон в формате +7XXXXXXXXXX");
            return "redirect:/client/profile";
        }

        if (clientService.existsByPhoneExceptId(phone, client.getId())) {
            ra.addFlashAttribute("error", "Телефон уже используется другим клиентом");
            return "redirect:/client/profile";
        }
        if (email != null && !email.isBlank()
                && clientService.existsByEmailExceptId(email, client.getId())) {
            ra.addFlashAttribute("error", "Email уже используется другим клиентом");
            return "redirect:/client/profile";
        }

        client.setLastName(lName);
        client.setFirstName(fName);
        client.setMiddleName(mName.isEmpty() ? null : mName);
        client.setEmail(email);
        client.setPhone(phone);
        clientService.save(client);

        boolean passwordChangeRequested = newPassword != null && !newPassword.isBlank();

        if (passwordChangeRequested) {
            if (currentPassword == null || currentPassword.isBlank()) {
                ra.addFlashAttribute("error", "Укажите текущий пароль для смены");
                return "redirect:/client/profile";
            }
            if (!userService.checkPassword(auth.getName(), currentPassword)) {
                ra.addFlashAttribute("error", "Текущий пароль неверный");
                return "redirect:/client/profile";
            }
            if (newPassword.length() < 6) {
                ra.addFlashAttribute("error", "Новый пароль должен быть не короче 6 символов");
                return "redirect:/client/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают");
                return "redirect:/client/profile";
            }
            userService.changePassword(auth.getName(), newPassword);
            ra.addFlashAttribute("success", "Профиль и пароль обновлены");
        } else {
            ra.addFlashAttribute("success", "Профиль обновлён");
        }

        return "redirect:/client/profile";
    }
}