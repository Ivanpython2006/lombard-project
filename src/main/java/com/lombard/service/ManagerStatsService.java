package com.lombard.service;

import com.lombard.model.Loan;
import com.lombard.model.LoanHistory;
import com.lombard.repository.LoanHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ManagerStatsService {

    private final LoanHistoryRepository historyRepository;

    public ManagerStatsService(LoanHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public List<LoanHistory> getAllActions(String username) {
        return historyRepository.findByChangedByOrderByChangedAtDesc(username);
    }

    public List<LoanHistory> getActionsBetween(String username,
                                               LocalDateTime from,
                                               LocalDateTime to) {
        return historyRepository.findByChangedByAndChangedAtBetweenOrderByChangedAtDesc(
                username, from, to);
    }

    // Считает статистику по списку действий
    private Map<String, Object> buildStats(List<LoanHistory> actions) {
        int approved = 0, rejected = 0, issued = 0, closed = 0, extended = 0, sold = 0;
        // заем выдается, когда клиент соглашается с условиями, - выдачу засчитываю менеджеру,
        // который одобрил заявку (каждый заем один раз, даже если одобрение записано дважды)
        Set<Long> issuedAfterApproval = new HashSet<>();

        for (LoanHistory h : actions) {
            String a = h.getAction() != null ? h.getAction().toLowerCase() : "";
            String target = a.contains("->") ? a.substring(a.lastIndexOf("->")) : a;
            if (a.startsWith("одобрено") || a.startsWith("ожидает подтверждения")) {
                approved++;
                Loan loan = h.getLoan();
                if (loan != null && loan.getId() != null && Boolean.TRUE.equals(loan.getClientApproved())) {
                    issuedAfterApproval.add(loan.getId());
                }
            }
            else if (a.startsWith("менеджер отозвал")) rejected++;
            else if (a.startsWith("статус") && target.contains("отклонен")) rejected++;
            // старые записи: раньше статус «Выдан» менеджер ставил сам
            else if (a.startsWith("статус") && target.contains("выдан")) issued++;
            else if (a.startsWith("статус") && target.contains("закрыт")) closed++;
            else if (a.startsWith("продлён") || a.startsWith("продлен")) extended++;
            else if (a.startsWith("статус") && (target.contains("продан")
                    || target.contains("на продаже") || target.contains("куплен"))) sold++;
        }
        issued += issuedAfterApproval.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", actions.size());
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("issued", issued);
        stats.put("closed", closed);
        stats.put("extended", extended);
        stats.put("sold", sold);
        return stats;
    }

    public Map<String, Object> getStatsForPeriod(String username,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {
        return buildStats(getActionsBetween(username, from, to));
    }

    public Map<String, Object> getStatsThisMonth(String username) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);
        return getStatsForPeriod(username, from, to);
    }

    public Map<String, Object> getStatsAllTime(String username) {
        return buildStats(getAllActions(username));
    }
}