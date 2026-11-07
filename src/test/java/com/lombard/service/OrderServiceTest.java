package com.lombard.service;

import com.lombard.model.CartItem;
import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.Order;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
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

//Тесты сервиса заказов: поиск заказов и оформление заказа из корзины.

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private NumberSequenceService numberSequenceService;

    private OrderService orderService;

    private Client client;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, loanRepository, numberSequenceService);
        client = new Client();
        client.setId(1L);
    }

    // товар в магазине = заем со статусом «На продаже» и одним залогом
    private Loan loanOnSale(long id) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setStatus("На продаже");
        Collateral c = new Collateral();
        c.setName("Телефон");
        c.setEstimatedPrice(new BigDecimal("1000"));
        loan.setCollaterals(new ArrayList<>(List.of(c)));
        return loan;
    }

    private Order checkout(List<CartItem> items, String delivery) {
        return orderService.createOrder(client, items, delivery, "Москва", "Ленина", "1", "2",
                null, "+79990000000", null, new BigDecimal("1000"));
    }

    @Test
    void findAll_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));

        assertEquals(1, orderService.findAll().size());
    }

    @Test
    void findByClientId_returnsClientOrders() {
        when(orderRepository.findByClientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        assertTrue(orderService.findByClientId(1L).isEmpty());
    }

    @Test
    void findById_missing_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(orderService.findById(99L));
    }

    @Test
    void save_withoutNumber_generatesNumber() {
        Order order = new Order();
        when(numberSequenceService.nextNumber("O", "O")).thenReturn("O5");

        orderService.save(order);

        assertEquals("O5", order.getOrderNumber());
        verify(orderRepository).save(order);
    }

    @Test
    void createOrder_emptyCart_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> checkout(List.of(), "PICKUP"));
    }

    // если товар уже купили или сняли с продажи - заказ не оформляется
    @Test
    void createOrder_loanNotOnSale_throwsException() {
        Loan loan = loanOnSale(2L);
        loan.setStatus("В ожидании");
        when(loanRepository.findById(2L)).thenReturn(Optional.of(loan));

        assertThrows(IllegalStateException.class, () -> checkout(List.of(new CartItem(client, loan)), "PICKUP"));
    }

    // самовывоз: подставляется адрес офиса товар становится статус куплен
    @Test
    void createOrder_pickup_marksLoanBought() {
        Loan loan = loanOnSale(2L);
        when(loanRepository.findById(2L)).thenReturn(Optional.of(loan));
        when(numberSequenceService.nextNumber("O", "O")).thenReturn("O1");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = checkout(List.of(new CartItem(client, loan)), "PICKUP");

        assertEquals("O1", order.getOrderNumber());
        assertEquals("Оплачен", order.getStatus());
        assertNotNull(order.getPickupAddress());
        assertEquals(1, order.getItems().size());
        assertEquals("Куплен", loan.getStatus());
    }

    // доставка: сохраняется адрес клиента
    @Test
    void createOrder_delivery_savesAddress() {
        Loan loan = loanOnSale(3L);
        when(loanRepository.findById(3L)).thenReturn(Optional.of(loan));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = checkout(List.of(new CartItem(client, loan)), "DELIVERY");

        assertEquals("Москва", order.getAddressCity());
        assertEquals("Ленина", order.getAddressStreet());
    }

    // у товара без залога имя и цена берутся по умолчанию
    @Test
    void createOrder_loanWithoutCollateral_usesDefaultName() {
        Loan loan = loanOnSale(4L);
        loan.setCollaterals(new ArrayList<>());
        when(loanRepository.findById(4L)).thenReturn(Optional.of(loan));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = checkout(List.of(new CartItem(client, loan)), "PICKUP");

        assertEquals(1, order.getItems().size());
    }
}
