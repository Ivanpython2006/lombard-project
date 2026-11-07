package com.lombard.service;

import com.lombard.model.CartItem;
import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartRepository;

    public CartService(CartItemRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public List<CartItem> getByClient(Long clientId) {
        return cartRepository.findByClientIdOrderByAddedAtDesc(clientId);
    }

    public long count(Long clientId) {
        return cartRepository.countByClientId(clientId);
    }

    public boolean contains(Long clientId, Long loanId) {
        return cartRepository.findByClientIdAndLoanId(clientId, loanId).isPresent();
    }

    @Transactional
    public void add(Client client, Loan loan) {
        if (loan == null || !"На продаже".equals(loan.getStatus())) {
            throw new IllegalArgumentException("Товар недоступен для покупки");
        }
        if (contains(client.getId(), loan.getId())) {
            throw new IllegalArgumentException("Товар уже в корзине");
        }
        cartRepository.save(new CartItem(client, loan));
    }

    @Transactional
    public void remove(Long cartItemId, Long clientId) {
        CartItem item = cartRepository.findById(cartItemId).orElse(null);
        if (item != null && item.getClient().getId().equals(clientId)) {
            cartRepository.delete(item);
        }
    }

    @Transactional
    public void clear(Long clientId) {
        cartRepository.deleteByClientId(clientId);
    }
}