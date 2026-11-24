package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.ClientService;
import com.lombard.service.CollateralService;
import com.lombard.service.LoanHistoryService;
import com.lombard.service.LoanService;
import com.lombard.service.NotificationService;
import com.lombard.service.UserService;
import com.lombard.util.CsvExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/loans")
public class LoanController {

    private static final int PAGE_SIZE = 10;

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry("В ожидании", Set.of("Отклонен", "Отменена")),
            Map.entry("Ожидает подтверждения", Set.of("Отклонен", "Отменена")),
            Map.entry("Выдан", Set.of("Закрыт", "Просрочен")),
            Map.entry("Просрочен", Set.of("Закрыт", "На складе", "На продаже")),
            Map.entry("На складе", Set.of("На продаже", "Закрыт")),
            Map.entry("На продаже", Set.of("Куплен", "На складе")),
            Map.entry("Закрыт", Set.of()),
            Map.entry("Отклонен", Set.of()),
            Map.entry("Отменена", Set.of()),
            Map.entry("Куплен", Set.of())
    );

    private static final Set<String> DELETABLE_STATUSES =
            Set.of("В ожидании", "Отклонен", "Отменена", "Закрыт");

    private final LoanService loanService;
    private final ClientService clientService;
    private final UserService userService;
    private final LoanHistoryService loanHistoryService;
    private final NotificationService notificationService;
    private final CollateralService collateralService;

    public LoanController(LoanService loanService,
                          ClientService clientService,
                          UserService userService,
                          LoanHistoryService loanHistoryService,
                          NotificationService notificationService,
                          CollateralService collateralService) {
        this.loanService = loanService;
        this.clientService = clientService;
        this.userService = userService;
        this.loanHistoryService = loanHistoryService;
        this.notificationService = notificationService;
        this.collateralService = collateralService;
    }

    private boolean isAdminOrManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    @GetMapping
    public String listLoans(@RequestParam(defaultValue = "id") String sort,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String clientName,
                            @RequestParam(defaultValue = "1") int page,
                            Model model, Authentication auth) {
        loanService.processOverdueLoans(notificationService);

        if (isAdminOrManager(auth)) {
            String statusFilter = (status == null || status.isEmpty()) ? null : status;
            List<Loan> loans = new ArrayList<>(loanService.searchLoans(statusFilter, clientName, null));

            switch (sort) {
                case "date_asc":
                    loans.sort(Comparator.comparing(Loan::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "amount_desc":
                    loans.sort(Comparator.comparing(Loan::getAmount, Comparator.nullsLast(Comparator.reverseOrder())));
                    break;
                case "amount_asc":
                    loans.sort(Comparator.comparing(Loan::getAmount, Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "status":
                    loans.sort(Comparator.comparing(Loan::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "id":
                default:
                    loans.sort(Comparator.comparing(Loan::getId).reversed());
                    break;
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
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("isAdminOrManager", true);
        } else {
            User user = userService.findByUsername(auth.getName());
            Client client = (user == null) ? null : clientService.findByUserId(user.getId());
            model.addAttribute("loans", client != null
                    ? loanService.findByClientId(client.getId())
                    : List.of());
            model.addAttribute("isAdminOrManager", false);
        }
        model.addAttribute("sort", sort);
        model.addAttribute("status", status);
        model.addAttribute("clientName", clientName);
        return "loans";
    }

    @GetMapping("/export")
    public void exportLoans(HttpServletResponse response,
                            Authentication auth) throws IOException {
        if (!isAdminOrManager(auth)) {
            response.sendRedirect("/403");
            return;
        }

        List<Loan> loans = loanService.findAll();
        List<String[]> rows = new ArrayList<>();

        for (Loan l : loans) {
            String collateralName = "";
            String collateralPrice = "";
            if (!l.getCollaterals().isEmpty()) {
                Collateral c = l.getCollaterals().get(0);
                collateralName = c.getName() != null ? c.getName() : "";
                collateralPrice = c.getEstimatedPrice() != null
                        ? c.getEstimatedPrice().toPlainString() : "";
            }

            rows.add(new String[]{
                    String.valueOf(l.getId()),
                    l.getClient() != null ? CsvExporter.esc(l.getClient().getFullName()) : "",
                    CsvExporter.esc(collateralName),
                    collateralPrice,
                    l.getAmount() != null ? l.getAmount().toPlainString() : "",
                    l.getRate() != null ? l.getRate().toPlainString() : "",
                    l.getRate() != null ? l.getTotalToReturn().toPlainString() : "",
                    l.getStatus() != null ? CsvExporter.esc(l.getStatus()) : "",
                    l.getCreatedDate() != null ? l.getCreatedDate().toString() : "",
                    l.getDueDate() != null ? l.getDueDate().toString() : ""
            });
        }

        CsvExporter.write(response, "loans.csv", rows,
                "ID", "Клиент", "Вещь", "Оценка", "Сумма",
                "Ставка %", "К возврату", "Статус", "Создан", "Вернуть до");
    }

    @GetMapping("/{id}")
    public String loanDetails(@PathVariable Long id, Model model, Authentication auth,
                              RedirectAttributes ra) {
        boolean isAdminOrManager = isAdminOrManager(auth);
        String backUrl = isAdminOrManager ? "redirect:/loans" : "redirect:/client/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return backUrl;
        }

        // Чтобы Thymeleaf не падал если photoUrls = null
        if (loan.getCollaterals() != null) {
            loan.getCollaterals().forEach(c -> {
                if (c.getPhotoUrls() == null) c.setPhotoUrls(new ArrayList<>());
            });
        }

        if (!isAdminOrManager) {
            User user = userService.findByUsername(auth.getName());
            Client client = (user == null) ? null : clientService.findByUserId(user.getId());
            if (loan.getClient() == null || client == null
                    || !loan.getClient().getId().equals(client.getId())) {
                ra.addFlashAttribute("error", "Заем не найден");
                return backUrl;
            }
        }
        model.addAttribute("loan", loan);
        model.addAttribute("isAdminOrManager", isAdminOrManager);
        model.addAttribute("history", loanHistoryService.findByLoanId(id));
        return "loan_details";
    }

    @GetMapping("/{id}/approve")
    public String approveForm(@PathVariable Long id, Model model, Authentication auth,
                              RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }
        if (!"В ожидании".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Оценить можно только новую заявку (статус «В ожидании»)");
            return "redirect:/loans/" + id;
        }
        model.addAttribute("loan", loan);
        return "loan_approve";
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public String approveLoan(@PathVariable Long id,
                              @RequestParam(defaultValue = "") String rate,
                              @RequestParam(defaultValue = "") String estimatedPrice,
                              @RequestParam(defaultValue = "") String approvedAmount,
                              @RequestParam(required = false) String collateralName,
                              @RequestParam(required = false) String collateralDescription,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";

        BigDecimal rateVal;
        BigDecimal estimatedPriceVal;
        BigDecimal approvedAmountVal;
        try {
            rateVal = new BigDecimal(rate.strip());
            estimatedPriceVal = new BigDecimal(estimatedPrice.strip());
            approvedAmountVal = new BigDecimal(approvedAmount.strip());
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("error", "Укажите числа в полях ставки, оценки и суммы займа");
            return "redirect:/loans/" + id + "/approve";
        }

        if (estimatedPriceVal.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("error", "Укажите корректную оценку вещи (больше 0)");
            return "redirect:/loans/" + id + "/approve";
        }
        if (rateVal.compareTo(BigDecimal.ZERO) < 0 || rateVal.compareTo(BigDecimal.valueOf(100)) > 0) {
            ra.addFlashAttribute("error", "Ставка должна быть в диапазоне 0-100%");
            return "redirect:/loans/" + id + "/approve";
        }

        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }
        // одобрить можно только новую заявку, иначе закрытый заем "ожил" бы заново
        if (!"В ожидании".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Оценить можно только новую заявку (статус «В ожидании»)");
            return "redirect:/loans/" + id;
        }

        if (approvedAmountVal.compareTo(BigDecimal.ONE) < 0) {
            ra.addFlashAttribute("error", "Сумма займа должна быть не меньше 1 ₽");
            return "redirect:/loans/" + id + "/approve";
        }

        BigDecimal safeAmount = approvedAmountVal;
        if (safeAmount.compareTo(estimatedPriceVal) > 0) {
            ra.addFlashAttribute("error", "Сумма займа не может быть больше оценки вещи");
            return "redirect:/loans/" + id + "/approve";
        }

        // запрошенную клиентом сумму (amount) не трогаю, одобренная хранится отдельно
        loan.setRate(rateVal);
        loan.setStatus("Ожидает подтверждения");
        loan.setAgreedAmount(safeAmount);
        loan.setClientApproved(null);
        loan.setDecisionDate(LocalDate.now());
        loanService.save(loan);

        StringBuilder changes = new StringBuilder();

        if (!loan.getCollaterals().isEmpty()) {
            Collateral c = loan.getCollaterals().get(0);
            String oldName = c.getName();
            String oldDesc = c.getDescription();

            c.setEstimatedPrice(estimatedPriceVal);

            if (collateralName != null && !collateralName.isBlank()) {
                String newName = collateralName.trim();
                if (oldName == null || !oldName.equals(newName)) {
                    changes.append("Название: «").append(oldName != null ? oldName : "-")
                            .append("» -> «").append(newName).append("». ");
                    c.setName(newName);
                }
            }

            if (collateralDescription != null) {
                String newDesc = collateralDescription.trim();
                String safeOld = oldDesc != null ? oldDesc : "";
                if (!safeOld.equals(newDesc)) {
                    changes.append("Описание: «").append(safeOld.isEmpty() ? "-" : safeOld)
                            .append("» -> «").append(newDesc.isEmpty() ? "-" : newDesc).append("». ");
                    c.setDescription(newDesc);
                }
            }
        }
        loanService.save(loan);

        String itemName = loanService.getItemName(loan);
        BigDecimal totalToReturn = safeAmount.add(
                safeAmount.multiply(rateVal).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        ).setScale(2, RoundingMode.HALF_UP);

        loanHistoryService.log(loan,
                "Ожидает подтверждения. Оценка: " + estimatedPriceVal.toPlainString() +
                        " руб., сумма: " + safeAmount.toPlainString() +
                        " руб., ставка: " + rateVal.toPlainString() + "%",
                auth.getName());

        if (loan.getClient() != null) {
            notificationService.notify(loan.getClient(), "LOAN_APPROVED", "WARNING",
                    itemName,
                    loan.getAmount() != null ? loan.getAmount().toPlainString() : "0",
                    safeAmount.toPlainString(),
                    rateVal.toPlainString(),
                    totalToReturn.toPlainString()
            );

            if (changes.length() > 0) {
                notificationService.notify(loan.getClient(), "LOAN_CHANGED", "WARNING",
                        loan.getLoanNumber(),
                        changes.toString().trim()
                );
            }
        }

        ra.addFlashAttribute("success", "Заявка одобрена и отправлена клиенту на подтверждение");
        return "redirect:/loans/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }
        model.addAttribute("loan", loan);
        // в выпадающем списке доступны только текущий статус и разрешенные переходы
        Set<String> allowedStatuses = new java.util.HashSet<>(
                ALLOWED_TRANSITIONS.getOrDefault(loan.getStatus(), Set.of()));
        allowedStatuses.add(loan.getStatus());
        model.addAttribute("allowedStatuses", allowedStatuses);
        return "edit_loan";
    }

    @PostMapping("/{id}/edit")
    public String editSave(@PathVariable Long id,
                           @RequestParam(required = false) BigDecimal amount,
                           @RequestParam(required = false) BigDecimal rate,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String dueDate,
                           @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                           Authentication auth,
                           RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }

        if (amount != null && amount.compareTo(BigDecimal.ONE) < 0) {
            ra.addFlashAttribute("error", "Сумма займа должна быть не меньше 1 ₽");
            return "redirect:/loans/" + id + "/edit";
        }
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            ra.addFlashAttribute("error", "Ставка должна быть в диапазоне 0-100%");
            return "redirect:/loans/" + id + "/edit";
        }

        BigDecimal estimated = loan.getCollaterals() != null && !loan.getCollaterals().isEmpty()
                ? loan.getCollaterals().get(0).getEstimatedPrice() : null;
        if (amount != null && estimated != null && amount.compareTo(estimated) > 0) {
            ra.addFlashAttribute("error", "Сумма займа не может быть больше оценки вещи");
            return "redirect:/loans/" + id + "/edit";
        }
        LocalDate parsedDue = null;
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                parsedDue = LocalDate.parse(dueDate);
                if (loan.getCreatedDate() != null && parsedDue.isBefore(loan.getCreatedDate())) {
                    ra.addFlashAttribute("error", "Дата возврата не может быть раньше даты создания займа");
                    return "redirect:/loans/" + id + "/edit";
                }
            } catch (DateTimeParseException e) {
                ra.addFlashAttribute("error", "Укажите корректную дату возврата");
                return "redirect:/loans/" + id + "/edit";
            }
        }

        // после одобрения правится одобренная сумма, до него - запрошенная
        if (amount != null) {
            if (loan.getAgreedAmount() != null) loan.setAgreedAmount(amount);
            else loan.setAmount(amount);
        }
        if (rate != null) loan.setRate(rate);
        // через форму редактирования действуют те же правила переходов, что и в changeStatus
        String oldStatus = loan.getStatus();
        if (status != null && !status.isBlank() && !status.equals(oldStatus)) {
            if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(status)) {
                ra.addFlashAttribute("error",
                        "Недопустимый переход: <" + oldStatus + "> -> <" + status + ">");
                return "redirect:/loans/" + id + "/edit";
            }
            loan.setStatus(status);
        }
        if (parsedDue != null) {
            loan.setDueDate(parsedDue);
        }

        loanService.save(loan);

        if (photos != null && photos.length > 0
                && loan.getCollaterals() != null
                && !loan.getCollaterals().isEmpty()) {
            try {
                collateralService.appendPhotos(loan.getCollaterals().get(0), photos,
                        "item_" + loan.getCollaterals().get(0).getId());
            } catch (RuntimeException e) {
                ra.addFlashAttribute("error", "Ошибка загрузки фото: " + e.getMessage());
                return "redirect:/loans/" + id + "/edit";
            }
        }

        String logAmount = (amount != null) ? amount.toPlainString() + " руб." : "не указана";
        String logRate = (rate != null) ? rate.toPlainString() + " %" : "не указана";
        String logStatus = (status != null && !status.isEmpty()) ? status : "без изменений";

        loanHistoryService.log(loan,
                "Отредактирован менеджером: сумма=" + logAmount +
                        ", ставка=" + logRate +
                        ", статус=" + logStatus,
                auth.getName());

        ra.addFlashAttribute("success", "Заем обновлен");
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String status,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }

        String oldStatus = loan.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(status)) {
            ra.addFlashAttribute("error",
                    "Недопустимый переход: <" + oldStatus + "> -> <" + status + ">");
            return "redirect:/loans/" + id;
        }

        loan.setStatus(status);
        loanService.save(loan);

        String itemName = loanService.getItemName(loan);

        loanHistoryService.log(loan,
                "Статус: «" + oldStatus + "» -> «" + status + "»",
                auth.getName());

        if (loan.getClient() != null) {
            switch (status) {
                case "Отклонен":
                    notificationService.notify(loan.getClient(), "LOAN_REJECTED", "DANGER",
                            loan.getLoanNumber(), itemName);
                    break;
                case "Закрыт":
                    notificationService.notify(loan.getClient(), "LOAN_CLOSED", "SUCCESS",
                            loan.getLoanNumber(), itemName);
                    break;
                case "На продаже":
                    notificationService.notify(loan.getClient(), "LOAN_ON_SALE", "WARNING",
                            loan.getLoanNumber(), itemName);
                    break;
                default:
                    notificationService.notify(loan.getClient(), "LOAN_STATUS", "INFO",
                            loan.getLoanNumber(), itemName, status);
                    break;
            }
        }

        ra.addFlashAttribute("success", "Статус изменен на «" + status + "»");
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String withdrawOffer(@PathVariable Long id,
                                Authentication auth,
                                RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }
        if (!"Ожидает подтверждения".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Предложение нельзя отозвать в текущем статусе");
            return "redirect:/loans/" + id;
        }

        loan.setStatus("Отклонен");
        loan.setClientApproved(false);
        loanService.save(loan);

        String itemName = loanService.getItemName(loan);

        loanHistoryService.log(loan,
                "Менеджер отозвал предложение по заявке",
                auth.getName());

        if (loan.getClient() != null) {
            notificationService.notify(loan.getClient(), "LOAN_OFFER_WITHDRAWN", "DANGER",
                    loan.getLoanNumber(), itemName);
        }

        ra.addFlashAttribute("success", "Предложение отозвано. Заявка отклонена");
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/extend")
    public String extendLoan(@PathVariable Long id,
                             @RequestParam(defaultValue = "1") int months,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        if (loan == null) {
            ra.addFlashAttribute("error", "Заем не найден");
            return "redirect:/loans";
        }
        if (!"Выдан".equals(loan.getStatus()) && !"Просрочен".equals(loan.getStatus())) {
            ra.addFlashAttribute("error", "Продлить можно только выданный или просроченный заем");
            return "redirect:/loans/" + id;
        }
        if (months < 1 || months > 12) {
            ra.addFlashAttribute("error", "Продлить можно на срок от 1 до 12 месяцев");
            return "redirect:/loans/" + id;
        }
        loanService.extendLoan(id, months);
        Loan updated = loanService.findById(id);
        String itemName = loanService.getItemName(updated);

        loanHistoryService.log(updated,
                "Продлен на " + months + " мес. Новая дата возврата: " + updated.getDueDate(),
                auth.getName());

        if (updated.getClient() != null) {
            notificationService.notify(updated.getClient(), "LOAN_EXTENDED", "INFO",
                    updated.getLoanNumber(),
                    itemName,
                    String.valueOf(months),
                    String.valueOf(updated.getDueDate()));
        }

        ra.addFlashAttribute("success", "Заем продлен на " + months + " мес.");
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteLoan(@PathVariable Long id,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String clientName,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (!isAdminOrManager(auth)) return "redirect:/loans";
        Loan loan = loanService.findById(id);
        // удалить можно только заявку без денег и залога в работе: новую отклоненную отмененную или закрытую
        if (loan != null && !DELETABLE_STATUSES.contains(loan.getStatus())) {
            ra.addFlashAttribute("error", "Нельзя удалить заем в статусе «" + loan.getStatus() + "»");
            return "redirect:/loans/" + id;
        }
        loanService.delete(id);
        ra.addFlashAttribute("success", "Заявка удалена");
        ra.addAttribute("page", page != null ? page : 1);
        if (sort != null && !sort.isEmpty()) ra.addAttribute("sort", sort);
        if (status != null && !status.isEmpty()) ra.addAttribute("status", status);
        if (clientName != null && !clientName.isEmpty()) ra.addAttribute("clientName", clientName);
        return "redirect:/loans";
    }
}
