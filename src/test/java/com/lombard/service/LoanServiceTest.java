package com.lombard.service;

import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.LoanHistory;
import com.lombard.model.OrderItem;
import com.lombard.repository.CartItemRepository;
import com.lombard.repository.CollateralRepository;
import com.lombard.repository.LoanHistoryRepository;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Тесты сервиса займов - главного сервиса ломбарда. Проверяем создание, продление, погашение, удаление займов статистику и ночную обработку просрочек.

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private NumberSequenceService numberSequenceService;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private LoanHistoryRepository loanHistoryRepository;

    @Mock
    private CollateralRepository collateralRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private LoanHistoryService loanHistoryService;

    @Mock
    private AccountService accountService;

    @Mock
    private NotificationService notificationService;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, numberSequenceService, cartItemRepository,
                orderItemRepository, loanHistoryRepository, collateralRepository, fileStorageService,
                loanHistoryService);
    }

    // выданный заем клиента с id = clientId: 1000 руб под 10%
    private Loan issuedLoan(long clientId) {
        Client c = new Client();
        c.setId(clientId);
        Loan l = new Loan();
        l.setId(10L);
        l.setClient(c);
        l.setStatus("Выдан");
        l.setAmount(new BigDecimal("1000"));
        l.setRate(new BigDecimal("10"));
        return l;
    }

    // заем с нужным статусом и сроком возврата через daysToDue дней (минус = уже прошел)
    private Loan loanDue(long id, String status, int daysToDue) {
        Loan l = issuedLoan(1L);
        l.setId(id);
        l.setStatus(status);
        l.setDueDate(LocalDate.now().plusDays(daysToDue));
        return l;
    }

    private LoanHistory history(String action) {
        LoanHistory h = new LoanHistory();
        h.setAction(action);
        return h;
    }

    // поиск и сортировка

    @Test
    void findAll_returnsAllLoans() {
        when(loanRepository.findAll()).thenReturn(List.of(new Loan()));

        assertEquals(1, loanService.findAll().size());
        assertEquals(1, loanService.getAllLoans().size());
    }

    @Test
    void findById_missing_returnsNull() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(loanService.findById(99L));
    }

    @Test
    void findByClientIdAndCount_callRepository() {
        when(loanRepository.findByClientId(1L)).thenReturn(List.of());
        when(loanRepository.count()).thenReturn(7L);

        assertTrue(loanService.findByClientId(1L).isEmpty());
        assertEquals(7L, loanService.count());
    }

    // 4 варианта сортировки: сумма дата статус и по умолчанию id
    @Test
    void findPaginated_allSortOptions_callRepository() {
        when(loanRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        loanService.findPaginated(1, 10, "amount");
        loanService.findPaginated(1, 10, "date");
        loanService.findPaginated(1, 10, "status");
        loanService.findPaginated(1, 10, "что-то другое");

        verify(loanRepository, times(4)).findAll(any(Pageable.class));
    }

    @Test
    void findAllSorted_allSortOptions_callRepository() {
        when(loanRepository.findAll(any(Sort.class))).thenReturn(new ArrayList<>());

        loanService.findAllSorted("amount");
        loanService.findAllSorted("date");
        loanService.findAllSorted("status");
        loanService.findAllSorted("id");

        verify(loanRepository, times(4)).findAll(any(Sort.class));
    }

    // ищу один и тот же заем по номеру фио номеру клиента, номеру вещи и дате
    @Test
    void searchLoans_byEachFilter_findsLoan() {
        Loan l = new Loan();
        l.setLoanNumber("L1");
        l.setStatus("Выдан");
        l.setCreatedDate(LocalDate.now());
        Client c = new Client();
        c.setFullName("Иванов");
        c.setClientNumber("C1");
        l.setClient(c);
        Collateral col = new Collateral();
        col.setItemNumber("M1");
        l.setCollaterals(List.of(col));
        when(loanRepository.findAll()).thenReturn(List.of(l));

        assertEquals(1, loanService.searchLoans("Выдан", null, null).size());
        assertEquals(1, loanService.searchLoans(null, "L1", null).size());
        assertEquals(1, loanService.searchLoans(null, "иванов", null).size());
        assertEquals(1, loanService.searchLoans(null, "C1", null).size());
        assertEquals(1, loanService.searchLoans(null, "M1", null).size());
        assertEquals(1, loanService.searchLoans(null, null, LocalDate.now().toString()).size());
    }

    @Test
    void searchLoans_noMatchOrBadDate_handlesCorrectly() {
        Loan l = new Loan();
        l.setStatus("Выдан");
        when(loanRepository.findAll()).thenReturn(List.of(l));

        assertTrue(loanService.searchLoans("Закрыт", "", "").isEmpty());
        assertTrue(loanService.searchLoans(null, "нет такого", null).isEmpty());
        // кривая дата просто игнорируется
        assertEquals(1, loanService.searchLoans(null, null, "не-дата").size());
    }

    // создание и изменение

    @Test
    void save_withoutNumber_generatesNumber() {
        Loan l = new Loan();
        when(numberSequenceService.nextNumber("L", "L")).thenReturn("L5");

        loanService.save(l);

        assertEquals("L5", l.getLoanNumber());
        verify(loanRepository).save(l);
    }

    @Test
    void createLoan_newLoan_setsDatesAndPendingStatus() {
        Loan l = new Loan();

        loanService.createLoan(l, 6);

        assertEquals(LocalDate.now(), l.getCreatedDate());
        assertEquals(LocalDate.now().plusMonths(6), l.getDueDate());
        assertEquals("В ожидании", l.getStatus());
        verify(loanRepository).save(l);
    }

    @Test
    void closeAndSell_existingLoan_changeStatus() {
        Loan l = new Loan();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        loanService.closeLoan(1L);
        assertEquals("Закрыт", l.getStatus());

        loanService.sellLoan(1L);
        assertEquals("На продаже", l.getStatus());
    }

    @Test
    void extendLoan_withDueDate_addsMonths() {
        Loan l = new Loan();
        l.setDueDate(LocalDate.now());
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        loanService.extendLoan(1L, 3);

        assertEquals(LocalDate.now().plusMonths(3), l.getDueDate());
        verify(loanRepository).save(l);
    }

    // если срока не было считаю от сегодняшнего дня
    @Test
    void extendLoan_withoutDueDate_countsFromToday() {
        Loan l = new Loan();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        loanService.extendLoan(1L, 3);

        assertEquals(LocalDate.now().plusMonths(3), l.getDueDate());
    }

    //перед удалением чистим корзины отвязал заказы, удалил историю и фото
    @Test
    void delete_existingLoan_cleansLinksAndPhotos() {
        Loan l = new Loan();
        l.setId(1L);
        Collateral c = new Collateral();
        c.setPhotoUrls(new ArrayList<>(List.of("/uploads/1.jpg")));
        l.setCollaterals(new ArrayList<>(List.of(c)));
        OrderItem oi = new OrderItem();
        oi.setLoan(l);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));
        when(orderItemRepository.findByLoanId(1L)).thenReturn(List.of(oi));

        loanService.delete(1L);

        assertNull(oi.getLoan());
        verify(cartItemRepository).deleteByLoanId(1L);
        verify(loanHistoryRepository).deleteByLoanId(1L);
        verify(fileStorageService).delete("/uploads/1.jpg");
        verify(loanRepository).delete(l);
    }

    @Test
    void delete_missingLoan_doesNothing() {
        when(loanRepository.findById(5L)).thenReturn(Optional.empty());

        loanService.delete(5L);

        verify(loanRepository, never()).delete(any());
    }
    //погашение займа
    @Test
    void repayLoan_enoughMoney_closesLoan() {
        Loan l = issuedLoan(1L);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(l));

        assertTrue(loanService.repayLoan(10L, 1L, accountService));
        assertEquals("Закрыт", l.getStatus());
        verify(accountService).withdraw(eq(1L), any(), anyString());
    }

    @Test
    void repayLoan_notEnoughMoney_returnsFalse() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(issuedLoan(1L)));
        doThrow(new IllegalArgumentException("Нет денег")).when(accountService).withdraw(any(), any(), any());

        assertFalse(loanService.repayLoan(10L, 1L, accountService));
    }

    // чужой заем погасить нельзя
    @Test
    void repayLoan_otherClient_returnsFalse() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(issuedLoan(1L)));

        assertFalse(loanService.repayLoan(10L, 2L, accountService));
        verifyNoInteractions(accountService);
    }

    @Test
    void repayLoan_closedOrMissing_returnsFalse() {
        Loan closed = issuedLoan(1L);
        closed.setStatus("Закрыт");
        when(loanRepository.findById(10L)).thenReturn(Optional.of(closed));
        when(loanRepository.findById(11L)).thenReturn(Optional.empty());

        assertFalse(loanService.repayLoan(10L, 1L, accountService));
        assertFalse(loanService.repayLoan(11L, 1L, accountService));
    }

    // если займов нет база вернёт null а мы должны вернуть 0 (статистика)
    @Test
    void averages_noData_returnZero() {
        when(loanRepository.getAverageLoanAmount()).thenReturn(null);
        when(loanRepository.findDecisionDates()).thenReturn(List.of());

        assertEquals(0, loanService.getAverageLoanAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0.0, loanService.getAverageWaitingDays());
    }

    // заявки ждали решения 2 и 4 дня - в среднем 3 дня
    @Test
    void averages_withData_returnValues() {
        LocalDate today = LocalDate.now();
        List<Object[]> dates = new ArrayList<>();
        dates.add(new Object[]{today.minusDays(2), today});
        dates.add(new Object[]{today.minusDays(4), today});
        when(loanRepository.getAverageLoanAmount()).thenReturn(100.0);
        when(loanRepository.findDecisionDates()).thenReturn(dates);

        assertEquals(BigDecimal.valueOf(100.0), loanService.getAverageLoanAmount());
        assertEquals(3.0, loanService.getAverageWaitingDays());
    }

    @Test
    void countByStatus_rows_convertedToMap() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Выдан", 5L});
        when(loanRepository.countByStatus()).thenReturn(rows);

        assertEquals(5L, loanService.countByStatus().get("Выдан"));
    }

    // сумма может прийти как BigDecimal как число как null или вообще мусор
    @Test
    void sumByStatus_differentValueTypes_convertedToBigDecimal() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Выдан", BigDecimal.TEN});
        rows.add(new Object[]{"Закрыт", null});
        rows.add(new Object[]{"Просрочен", 5});
        rows.add(new Object[]{"Другой", "строка"});
        when(loanRepository.sumByStatus()).thenReturn(rows);

        Map<String, BigDecimal> result = loanService.sumByStatus();

        assertEquals(0, result.get("Выдан").compareTo(BigDecimal.TEN));
        assertEquals(0, result.get("Закрыт").compareTo(BigDecimal.ZERO));
        assertEquals(0, result.get("Просрочен").compareTo(new BigDecimal("5")));
        assertEquals(0, result.get("Другой").compareTo(BigDecimal.ZERO));
    }

    @Test
    void getItemName_differentLoans_returnsName() {
        Loan withName = new Loan();
        Collateral c = new Collateral();
        c.setName("Телефон");
        withName.setCollaterals(List.of(c));

        assertEquals("Телефон", loanService.getItemName(withName));
        assertEquals("Товар", loanService.getItemName(new Loan()));
        assertEquals("-", loanService.getItemName(null));
    }

    // напоминания за 3 дня, за 1 день и в последний день (обработка просрочек)
    @Test
    void processOverdue_dueSoon_sendsReminders() {
        Loan in3 = loanDue(1L, "Выдан", 3);
        Loan in1 = loanDue(2L, "Выдан", 1);
        Loan today = loanDue(3L, "Выдан", 0);
        Loan in10 = loanDue(4L, "Выдан", 10);
        when(loanRepository.findAll()).thenReturn(List.of(in3, in1, today, in10));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(anyLong())).thenReturn(List.of());

        loanService.processOverdueLoans(notificationService);

        verify(notificationService, times(3)).notify(any(), eq("LOAN_REMINDER"), any(), any(String[].class));
        verify(loanHistoryService, times(3)).log(any(), anyString(), eq("SYSTEM"));
    }

    // второй раз то же напоминание не отправляем
    @Test
    void processOverdue_reminderAlreadySent_doesNotRepeat() {
        Loan in3 = loanDue(1L, "Выдан", 3);
        when(loanRepository.findAll()).thenReturn(List.of(in3));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(1L))
                .thenReturn(List.of(history("Напоминание 3 дн. до срока возврата")));

        loanService.processOverdueLoans(notificationService);

        verifyNoInteractions(notificationService);
    }

    // срок прошел - заем становится просроченным уведомляем клиента и сотрудников
    @Test
    void processOverdue_pastDue_marksOverdueAndNotifies() {
        Loan late = loanDue(1L, "Выдан", -1);
        when(loanRepository.findAll()).thenReturn(List.of(late));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(1L)).thenReturn(List.of());

        loanService.processOverdueLoans(notificationService);

        assertEquals("Просрочен", late.getStatus());
        verify(notificationService).notify(any(), eq("LOAN_OVERDUE"), any(), any(String[].class));
        verify(notificationService).notifyStaff(eq("LOAN_OVERDUE_STAFF"), any(), any(String[].class));
    }

    // если уведомление о просрочке уже было статус меняем но не спамим
    @Test
    void processOverdue_overdueAlreadyNotified_onlyChangesStatus() {
        Loan late = loanDue(1L, "Выдан", -2);
        late.setClient(null);
        when(loanRepository.findAll()).thenReturn(List.of(late));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(1L))
                .thenReturn(List.of(history("Просрочен. Ожидает оплаты.")));

        loanService.processOverdueLoans(notificationService);

        assertEquals("Просрочен", late.getStatus());
        verify(notificationService, never()).notifyStaff(any(), any(), any(String[].class));
    }

    // просрочка 30+ дней - вещь уходит на склад
    @Test
    void processOverdue_overdueMoreThan30Days_movesToWarehouse() {
        Loan old = loanDue(1L, "Просрочен", -40);
        Loan noClient = loanDue(2L, "Просрочен", -35);
        noClient.setClient(null);
        when(loanRepository.findAll()).thenReturn(List.of(old, noClient));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(anyLong())).thenReturn(List.of());

        loanService.processOverdueLoans(notificationService);

        assertEquals("На складе", old.getStatus());
        assertEquals("На складе", noClient.getStatus());
        verify(notificationService, times(2)).notifyStaff(eq("LOAN_ON_WAREHOUSE_STAFF"), any(), any(String[].class));
    }

    @Test
    void processOverdue_alreadyOnWarehouseOrNoDate_skips() {
        Loan moved = loanDue(1L, "Просрочен", -40);
        Loan noDate = issuedLoan(1L);
        noDate.setId(2L);
        when(loanRepository.findAll()).thenReturn(List.of(moved, noDate));
        when(loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(1L))
                .thenReturn(List.of(history("Переведен на склад (просрочка 40 дн.)")));

        loanService.processOverdueLoans(notificationService);

        assertEquals("Просрочен", moved.getStatus());
        verifyNoInteractions(notificationService);
    }
}
