package com.lombard.service;

import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.repository.ClientRepository;
import com.lombard.repository.LoanRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Тесты сервиса клиентов. Репозитории заменены моками база не нужна

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private NumberSequenceService numberSequenceService;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, loanRepository, numberSequenceService);
    }

    // если номера нет сервис сам берет следующий номер вида C...
    @Test
    void save_withoutNumber_generatesNumber() {
        Client c = new Client();
        when(numberSequenceService.nextNumber("C", "C")).thenReturn("C1");
        when(clientRepository.save(c)).thenReturn(c);

        Client saved = clientService.addClient(c);

        assertEquals("C1", saved.getClientNumber());
    }

    // уже выданный номер перезаписываться не должен
    @Test
    void save_withNumber_keepsNumber() {
        Client c = new Client();
        c.setClientNumber("C77");
        when(clientRepository.save(c)).thenReturn(c);

        clientService.save(c);

        assertEquals("C77", c.getClientNumber());
        verifyNoInteractions(numberSequenceService);
    }

    @Test
    void findAll_returnsAllClients() {
        Client c = new Client();
        when(clientRepository.findAll()).thenReturn(List.of(c));

        assertEquals(1, clientService.findAll().size());
        assertEquals(1, clientService.getAllClients().size());
    }

    @Test
    void findById_existing_returnsClient() {
        Client c = new Client();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));

        assertSame(c, clientService.findById(1L));
    }

    @Test
    void findById_missing_returnsNull() {
        when(clientRepository.findById(2L)).thenReturn(Optional.empty());

        assertNull(clientService.findById(2L));
    }

    @Test
    void findByUserId_existing_returnsClient() {
        Client c = new Client();
        when(clientRepository.findByUserId(1L)).thenReturn(Optional.of(c));

        assertSame(c, clientService.findByUserId(1L));
    }

    // поиск по фио и по всем полям просто идет в репозиторий
    @Test
    void search_byNameAndAnyField_callsRepository() {
        when(clientRepository.findByFullNameContainingIgnoreCase("иван")).thenReturn(List.of(new Client()));
        when(clientRepository.searchByAnyField("+7")).thenReturn(List.of());

        assertEquals(1, clientService.searchClients("иван").size());
        assertEquals(1, clientService.searchByName("иван").size());
        assertTrue(clientService.searchByAnyField("+7").isEmpty());
    }

    @Test
    void count_returnsRepositoryCount() {
        when(clientRepository.count()).thenReturn(3L);

        assertEquals(3L, clientService.count());
    }

    // пустой телефон/почту даже не ищем в базе
    @Test
    void exists_emptyPhoneOrEmail_returnsFalse() {
        assertFalse(clientService.existsByPhone(null));
        assertFalse(clientService.existsByPhone(" "));
        assertFalse(clientService.existsByEmail(null));
        assertFalse(clientService.existsByEmail(""));
        assertFalse(clientService.existsByPhoneExceptId(null, 1L));
        assertFalse(clientService.existsByEmailExceptId("", 1L));
        verifyNoInteractions(clientRepository);
    }

    @Test
    void exists_takenPhoneOrEmail_returnsTrue() {
        when(clientRepository.existsByPhone("+7900")).thenReturn(true);
        when(clientRepository.existsByEmail("a@a.ru")).thenReturn(true);

        assertTrue(clientService.existsByPhone("+7900"));
        assertTrue(clientService.existsByEmail("a@a.ru"));
    }

    // при редактировании клиента его собственный телефон не считается занятым
    @Test
    void existsExceptId_takenByOther_returnsTrue() {
        when(clientRepository.existsByPhoneAndIdNot("+7900", 1L)).thenReturn(true);
        when(clientRepository.existsByEmailAndIdNot("a@a.ru", 1L)).thenReturn(true);

        assertTrue(clientService.existsByPhoneExceptId("+7900", 1L));
        assertTrue(clientService.existsByEmailExceptId("a@a.ru", 1L));
    }

    // клиента с займами удалять нельзя
    @Test
    void deleteClient_withLoans_throwsException() {
        when(loanRepository.findByClientId(1L)).thenReturn(List.of(new Loan()));

        assertThrows(IllegalStateException.class, () -> clientService.deleteClient(1L));
        verify(clientRepository, never()).deleteById(any());
    }

    @Test
    void deleteClient_withoutLoans_deletes() {
        when(loanRepository.findByClientId(1L)).thenReturn(List.of());

        clientService.deleteClient(1L);

        verify(clientRepository).deleteById(1L);
    }
}
