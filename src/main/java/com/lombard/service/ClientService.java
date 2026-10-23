package com.lombard.service;

import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.repository.ClientRepository;
import com.lombard.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final LoanRepository loanRepository;
    private final NumberSequenceService numberSequenceService;

    public ClientService(ClientRepository clientRepository,
                         LoanRepository loanRepository,
                         NumberSequenceService numberSequenceService) {
        this.clientRepository = clientRepository;
        this.loanRepository = loanRepository;
        this.numberSequenceService = numberSequenceService;
    }

    public Client addClient(Client client) {
        return save(client);
    }

    public Client save(Client client) {
        if (client.getClientNumber() == null || client.getClientNumber().isBlank()) {
            client.setClientNumber(numberSequenceService.nextNumber("C", "C"));
        }
        return clientRepository.save(client);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client findById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    public Client findByUserId(Long userId) {
        return clientRepository.findByUserId(userId).orElse(null);
    }

    public List<Client> searchClients(String name) {
        return clientRepository.findByFullNameContainingIgnoreCase(name);
    }

    public List<Client> searchByName(String name) {
        return clientRepository.findByFullNameContainingIgnoreCase(name);
    }

    public List<Client> searchByAnyField(String query) {
        return clientRepository.searchByAnyField(query);
    }

    @Transactional
    public void deleteClient(Long id) {
        List<Loan> loans = loanRepository.findByClientId(id);
        if (loans != null && !loans.isEmpty()) {
            throw new IllegalStateException(
                    "Нельзя удалить клиента: у него " + loans.size() +
                    " заём(ов). Сначала удалите займы.");
        }
        clientRepository.deleteById(id);
    }

    public long count() {
        return clientRepository.count();
    }

    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return clientRepository.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return clientRepository.existsByEmail(email);
    }

    public boolean existsByPhoneExceptId(String phone, Long id) {
        if (phone == null || phone.isBlank()) return false;
        return clientRepository.existsByPhoneAndIdNot(phone, id);
    }

    public boolean existsByEmailExceptId(String email, Long id) {
        if (email == null || email.isBlank()) return false;
        return clientRepository.existsByEmailAndIdNot(email, id);
    }
}