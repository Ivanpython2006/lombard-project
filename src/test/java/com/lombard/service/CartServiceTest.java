package com.lombard.service;

import com.lombard.model.CartItem;
import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.repository.CartItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// тесты корзины: добавлять можно только товары «На продаже» и только один раз.

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository);
    }

    @Test
    void add_loanNotOnSale_throwsException() {
        Client client = new Client(); client.setId(1L);
        Loan loan = new Loan(); loan.setId(2L); loan.setStatus("Выдан");
        assertThatThrownBy(() -> cartService.add(client, loan)).isInstanceOf(IllegalArgumentException.class);
    }

    // один и тот же товар дважды в корзину не кладем
    @Test
    void add_alreadyInCart_throwsException() {
        Client client = new Client(); client.setId(1L);
        Loan loan = new Loan(); loan.setId(2L); loan.setStatus("На продаже");
        when(cartItemRepository.findByClientIdAndLoanId(1L, 2L)).thenReturn(Optional.of(new CartItem(client, loan)));
        assertThatThrownBy(() -> cartService.add(client, loan)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_loanOnSale_savesItem() {
        Client client = new Client(); client.setId(1L);
        Loan loan = new Loan(); loan.setId(2L); loan.setStatus("На продаже");
        when(cartItemRepository.findByClientIdAndLoanId(1L, 2L)).thenReturn(Optional.empty());
        cartService.add(client, loan);
        verify(cartItemRepository).save(any());
    }

    // удалить чужую позицию из корзины нельзя
    @Test
    void remove_otherClientItem_doesNotDelete() {
        CartItem item = new CartItem(); item.setId(5L);
        item.setClient(new Client()); item.getClient().setId(1L);
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        cartService.remove(5L, 999L);
        verify(cartItemRepository, never()).delete(any());
    }
}
