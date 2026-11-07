package com.lombard.service;

import com.lombard.model.*;
import com.lombard.model.LoanHistory;
import com.lombard.repository.*;
import com.lombard.repository.LoanHistoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//Тесты статистики менеджера: считаем его действия по истории займов
@ExtendWith(MockitoExtension.class)
class ManagerStatsServiceTest {

    @Mock
    private LoanHistoryRepository loanHistoryRepository;

    private ManagerStatsService managerStatsService;

    @BeforeEach
    void setUp() {
        managerStatsService = new ManagerStatsService(loanHistoryRepository);
    }

    @Test
    void statsAllTime_approvedAction_countsApproved() {
        LoanHistory h = new LoanHistory();
        h.setAction("одобрено");
        when(loanHistoryRepository.findByChangedByOrderByChangedAtDesc("user")).thenReturn(List.of(h));
        Map<String, Object> stats = managerStatsService.getStatsAllTime("user");
        assertThat(stats.get("total")).isEqualTo(1);
        assertThat(stats.get("approved")).isEqualTo(1);
    }

    @Test
    void statsAllTime_rejectedAction_countsTotal() {
        LoanHistory h = new LoanHistory();
        h.setAction("Статус: «Выдан» -> «Отклонен»");
        when(loanHistoryRepository.findByChangedByOrderByChangedAtDesc("m")).thenReturn(List.of(h));
        Map<String, Object> s = managerStatsService.getStatsAllTime("m");
        assertThat(s.get("total")).isEqualTo(1);
    }

    @Test
    void statsAllTime_issuedAction_countsIssued() {
        LoanHistory h = new LoanHistory();
        h.setAction("Статус: «Ожидает» -> «Выдан»");
        when(loanHistoryRepository.findByChangedByOrderByChangedAtDesc("m")).thenReturn(List.of(h));
        Map<String, Object> s = managerStatsService.getStatsAllTime("m");
        assertThat((Integer) s.get("issued")).isEqualTo(1);
    }

    // в истории по одному действию каждого типа
    @Test
    void statsAllTime_differentActions_countsEachType() {
        List<LoanHistory> actions = List.of(
                createHistory("Одобрено"),
                createHistory("Статус: отклонен"),
                createHistory("Статус: выдан"),
                createHistory("Статус: закрыт"),
                createHistory("Продлён"),
                createHistory("Статус: продан"),
                createHistory(null)
        );
        when(loanHistoryRepository.findByChangedByOrderByChangedAtDesc("user")).thenReturn(actions);

        Map<String, Object> stats = managerStatsService.getStatsAllTime("user");

        assertThat(stats.get("total")).isEqualTo(7);
        assertThat(stats.get("approved")).isEqualTo(1);
        assertThat(stats.get("rejected")).isEqualTo(1);
        assertThat(stats.get("issued")).isEqualTo(1);
        assertThat(stats.get("closed")).isEqualTo(1);
        assertThat(stats.get("extended")).isEqualTo(1);
        assertThat(stats.get("sold")).isEqualTo(1);
    }

    // за месяц берём историю только за нужный период
    @Test
    void statsThisMonth_callsRepositoryWithPeriod() {
        when(loanHistoryRepository.findByChangedByAndChangedAtBetweenOrderByChangedAtDesc(anyString(), any(), any()))
                .thenReturn(List.of());

        managerStatsService.getStatsThisMonth("user");
        verify(loanHistoryRepository).findByChangedByAndChangedAtBetweenOrderByChangedAtDesc(eq("user"), any(), any());
    }

    @Test
    void statsForPeriod_noActions_returnsZeros() {
        when(loanHistoryRepository.findByChangedByAndChangedAtBetweenOrderByChangedAtDesc(anyString(), any(), any()))
                .thenReturn(new ArrayList<>());
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        assertTrue(managerStatsService.getActionsBetween("user", from, to).isEmpty());
        assertEquals(0, managerStatsService.getStatsForPeriod("user", from, to).get("total"));
    }

    private LoanHistory createHistory(String action) {
        LoanHistory h = new LoanHistory();
        h.setAction(action);
        return h;
    }
}
