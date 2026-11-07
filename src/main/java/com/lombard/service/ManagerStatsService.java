package com.lombard.service;

import com.lombard.model.LoanHistory;
import com.lombard.repository.LoanHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        for (LoanHistory h : actions) {
            String a = h.getAction() != null ? h.getAction().toLowerCase() : "";
            if (a.startsWith("одобрено")) approved++;
            else if (a.startsWith("статус") && a.contains("отклонен")) rejected++;
            else if (a.startsWith("статус") && a.contains("выдан")) issued++;
            else if (a.startsWith("статус") && a.contains("закрыт")) closed++;
            else if (a.startsWith("продлён") || a.startsWith("продлен")) extended++;
            else if (a.startsWith("статус") && a.contains("продан")) sold++;
        }

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