package com.lombard.service;

import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.OrderItem;
import com.lombard.repository.CartItemRepository;
import com.lombard.repository.CollateralRepository;
import com.lombard.repository.LoanHistoryRepository;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.OrderItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    private static final int OVERDUE_TO_WAREHOUSE_DAYS = 30; // через сколько дней просрочки перевод залога на склад

    private final LoanRepository loanRepository;
    private final NumberSequenceService numberSequenceService;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final CollateralRepository collateralRepository;
    private final FileStorageService fileStorageService;
    private final LoanHistoryService loanHistoryService;

    public LoanService(LoanRepository loanRepository,
                       NumberSequenceService numberSequenceService,
                       CartItemRepository cartItemRepository,
                       OrderItemRepository orderItemRepository,
                       LoanHistoryRepository loanHistoryRepository,
                       CollateralRepository collateralRepository,
                       FileStorageService fileStorageService,
                       LoanHistoryService loanHistoryService) {
        this.loanRepository = loanRepository;
        this.numberSequenceService = numberSequenceService;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.loanHistoryRepository = loanHistoryRepository;
        this.collateralRepository = collateralRepository;
        this.fileStorageService = fileStorageService;
        this.loanHistoryService = loanHistoryService;
    }

    public List<Loan> findAll() { return loanRepository.findAll(); }
    public List<Loan> getAllLoans() { return loanRepository.findAll(); }

    public Page<Loan> findPaginated(int pageNo, int pageSize, String sort) {
        Sort s;
        switch (sort) {
            case "amount": s = Sort.by("amount"); break;
            case "date":   s = Sort.by("createdDate"); break;
            case "status": s = Sort.by("status"); break;
            default:       s = Sort.by("id"); break;
        }
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, s);
        return loanRepository.findAll(pageable);
    }

    public List<Loan> findAllSorted(String sort) {
        switch (sort) {
            case "amount": return loanRepository.findAll(Sort.by("amount"));
            case "date":   return loanRepository.findAll(Sort.by("createdDate"));
            case "status": return loanRepository.findAll(Sort.by("status"));
            default:       return loanRepository.findAll(Sort.by("id"));
        }
    }

    @Transactional(readOnly = true)
    public Loan findById(Long id) { return loanRepository.findById(id).orElse(null); }

    @Transactional
    public void save(Loan loan) {
        // если у займа еще нет номера то генерирую через sequence
        if (loan.getLoanNumber() == null || loan.getLoanNumber().isBlank()) {
            loan.setLoanNumber(numberSequenceService.nextNumber("L", "L"));
        }
        loanRepository.save(loan);
    }

    @Transactional
    public void delete(Long id) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan == null) return;

        cartItemRepository.deleteByLoanId(id); // чищу связи перед удалением

        List<OrderItem> orderItems = orderItemRepository.findByLoanId(id);
        for (OrderItem oi : orderItems) {
            oi.setLoan(null);
        }
        orderItemRepository.saveAll(orderItems);

        loanHistoryRepository.deleteByLoanId(id);

        // удаляю фотки с диска
        for (Collateral c : loan.getCollaterals()) {
            if (c.getPhotoUrls() != null) {
                for (String url : c.getPhotoUrls()) {
                    fileStorageService.delete(url);
                }
            }
        }

        loanRepository.delete(loan);
    }

    @Transactional(readOnly = true)
    public List<Loan> findByClientId(Long clientId) { return loanRepository.findByClientId(clientId); }
    public long count() { return loanRepository.count(); }

    public BigDecimal getAverageLoanAmount() {
        Double avg = loanRepository.getAverageLoanAmount();
        return avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg);
    }

    public Double getAverageWaitingDays() {
        Double avg = loanRepository.getAverageWaitingDays();
        return avg == null ? 0.0 : avg;
    }

    public Map<String, Long> countByStatus() {
        List<Object[]> results = loanRepository.countByStatus();
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    public Map<String, BigDecimal> sumByStatus() {
        List<Object[]> results = loanRepository.sumByStatus();
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            String status = (String) row[0];
            Object value = row[1];
            BigDecimal sum;
            if (value == null) {
                sum = BigDecimal.ZERO;
            } else if (value instanceof BigDecimal) {
                sum = (BigDecimal) value;
            } else if (value instanceof Number) {
                sum = BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                sum = BigDecimal.ZERO;
            }
            map.put(status, sum);
        }
        return map;
    }

    public List<Loan> searchLoans(String status, String search, String date) {
        LocalDate parsedDate = null;
        if (date != null && !date.isEmpty()) {
            try { parsedDate = LocalDate.parse(date); } catch (Exception ignored) {}
        }
        final LocalDate filterDate = parsedDate;

        // фильтр по статусу поисковой строке и дате
        return loanRepository.findAll().stream()
                .filter(l -> status == null || status.isEmpty() || status.equals(l.getStatus()))
                .filter(l -> {
                    if (search == null || search.isEmpty()) return true;
                    String s = search.toLowerCase().trim();
                    if (l.getLoanNumber() != null && l.getLoanNumber().toLowerCase().contains(s)) return true;
                    if (l.getClient() != null && l.getClient().getFullName() != null
                            && l.getClient().getFullName().toLowerCase().contains(s)) return true;
                    if (l.getClient() != null && l.getClient().getClientNumber() != null
                            && l.getClient().getClientNumber().toLowerCase().contains(s)) return true;
                    if (!l.getCollaterals().isEmpty()
                            && l.getCollaterals().get(0).getItemNumber() != null
                            && l.getCollaterals().get(0).getItemNumber().toLowerCase().contains(s)) return true;
                    return false;
                })
                .filter(l -> filterDate == null
                        || (l.getCreatedDate() != null && l.getCreatedDate().equals(filterDate)))
                .toList();
    }

    @Transactional
    public Loan createLoan(Loan loan, int months) {
        loan.setCreatedDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusMonths(months));
        if (loan.getStatus() == null || loan.getStatus().isEmpty()) loan.setStatus("В ожидании");
        return loanRepository.save(loan);
    }

    @Transactional
    public void closeLoan(Long id) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan != null) { loan.setStatus("Закрыт"); loanRepository.save(loan); }
    }

    @Transactional
    public void sellLoan(Long id) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan != null) { loan.setStatus("На продаже"); loanRepository.save(loan); }
    }

    @Transactional
    public void extendLoan(Long id, int months) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan != null) {
            // если дата возврата не задана - пляшу от текущей
            LocalDate base = loan.getDueDate() != null ? loan.getDueDate() : LocalDate.now();
            loan.setDueDate(base.plusMonths(months));
            loanRepository.save(loan);
        }
    }

    @Transactional
    public void processOverdueLoans(NotificationService notificationService) {
        LocalDate today = LocalDate.now();
        for (Loan l : loanRepository.findAll()) {
            if (l.getDueDate() == null) continue;
            String itemName = getItemName(l);

            // напоминаю за 3 дня за 1 день и в день возврата
            if ("Выдан".equals(l.getStatus())) {
                long daysLeft = l.getDaysLeft();
                if (daysLeft == 3 || daysLeft == 1 || daysLeft == 0) {
                    String whenKey;
                    if (daysLeft == 0) whenKey = "сегодня последний день";
                    else if (daysLeft == 1) whenKey = "остался 1 день";
                    else whenKey = "осталось 3 дня";

                    if (l.getClient() != null && !reminderAlreadySent(l.getId(), daysLeft)) {
                        notificationService.notify(l.getClient(), "LOAN_REMINDER", "WARNING",
                                l.getLoanNumber(), itemName, whenKey,
                                l.getTotalToReturn().toPlainString(),
                                String.valueOf(l.getDueDate()));
                        loanHistoryService.log(l,
                                "Напоминание " + daysLeft + " дн. до срока возврата",
                                "SYSTEM");
                    }
                }
            }

            // срок вышел перевожу в просроченные
            if ("Выдан".equals(l.getStatus()) && l.getDueDate().isBefore(today)) {
                l.setStatus("Просрочен");
                loanRepository.save(l);

                if (!overdueAlreadySent(l.getId())) {
                    if (l.getClient() != null) {
                        notificationService.notify(l.getClient(), "LOAN_OVERDUE", "DANGER",
                                l.getLoanNumber(), itemName, String.valueOf(l.getDueDate()));
                    }
                    notificationService.notifyStaff("LOAN_OVERDUE_STAFF", "DANGER",
                            l.getLoanNumber(), itemName,
                            l.getClient() != null ? l.getClient().getFullName() : "-");
                    loanHistoryService.log(l, "Просрочен. Ожидает оплаты.", "SYSTEM");
                }
            }

            // если просрочка больше 30 дней (залог уходит на склад)
            if ("Просрочен".equals(l.getStatus())) {
                long daysOverdue = ChronoUnit.DAYS.between(l.getDueDate(), today);
                if (daysOverdue >= OVERDUE_TO_WAREHOUSE_DAYS && !movedToWarehouse(l.getId())) {
                    l.setStatus("На складе");
                    loanRepository.save(l);

                    if (l.getClient() != null) {
                        notificationService.notify(l.getClient(), "LOAN_ON_WAREHOUSE", "DANGER",
                                l.getLoanNumber(), itemName,
                                String.valueOf(daysOverdue));
                    }
                    notificationService.notifyStaff("LOAN_ON_WAREHOUSE_STAFF", "DANGER",
                            l.getLoanNumber(), itemName,
                            l.getClient() != null ? l.getClient().getFullName() : "-");
                    loanHistoryService.log(l,
                            "Переведен на склад (просрочка " + daysOverdue + " дн.)",
                            "SYSTEM");
                }
            }
        }
    }

    // проверки чтобы не спамить уведомлениями
    private boolean overdueAlreadySent(Long loanId) {
        return loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(loanId).stream()
                .anyMatch(h -> h.getAction() != null
                        && h.getAction().toLowerCase().startsWith("просрочен"));
    }

    private boolean reminderAlreadySent(Long loanId, long daysLeft) {
        String marker = "напоминание " + daysLeft;
        return loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(loanId).stream()
                .anyMatch(h -> h.getAction() != null
                        && h.getAction().toLowerCase().contains(marker));
    }

    private boolean movedToWarehouse(Long loanId) {
        return loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(loanId).stream()
                .anyMatch(h -> h.getAction() != null
                        && h.getAction().toLowerCase().contains("переведен на склад"));
    }

    @Transactional
    public boolean repayLoan(Long loanId, Long clientId, AccountService accountService) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null || loan.getClient() == null
                || !loan.getClient().getId().equals(clientId)) return false;
        if (!"Выдан".equals(loan.getStatus()) && !"Просрочен".equals(loan.getStatus())) return false;

        try {
            accountService.withdraw(clientId, loan.getTotalToReturn(),
                    "Погашение займа " + loan.getLoanNumber());
        } catch (RuntimeException e) { return false; }

        loan.setStatus("Закрыт");
        loanRepository.save(loan);
        return true;
    }

    // беру название вещи из первого залога если есть
    public String getItemName(Loan loan) {
        if (loan == null) return "-";
        if (!loan.getCollaterals().isEmpty()
                && loan.getCollaterals().get(0).getName() != null
                && !loan.getCollaterals().get(0).getName().isBlank()) {
            return loan.getCollaterals().get(0).getName();
        }
        return "Товар";
    }
}