package com.lombard.service;

import com.lombard.model.*;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    // какие статусы заказа можно ставить после текущего
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "Оплачен", Set.of("Собран", "Отменён"),
            "Собран", Set.of("Доставлен", "Отменён"),
            "Доставлен", Set.of(),
            "Отменён", Set.of()
    );

    private final OrderRepository orderRepository;
    private final LoanRepository loanRepository;
    private final NumberSequenceService numberSequenceService;
    private final AccountService accountService;

    public OrderService(OrderRepository orderRepository,
                        LoanRepository loanRepository,
                        NumberSequenceService numberSequenceService,
                        AccountService accountService) {
        this.orderRepository = orderRepository;
        this.loanRepository = loanRepository;
        this.numberSequenceService = numberSequenceService;
        this.accountService = accountService;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public List<Order> findByClientId(Long clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public void save(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(numberSequenceService.nextNumber("O", "O"));
        }
        orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(Client client, List<CartItem> cartItems,
                             String deliveryType,
                             String city, String street, String building, String apartment,
                             String pickupAddress, String contactPhone, String comment,
                             BigDecimal totalAmount) {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Корзина пуста");
        }

        Order order = new Order();
        order.setClient(client);
        order.setTotalAmount(totalAmount);
        order.setStatus("Оплачен");
        order.setDeliveryType(deliveryType);
        order.setContactPhone(contactPhone);
        order.setComment(comment);

        if ("DELIVERY".equals(deliveryType)) {
            order.setAddressCity(city);
            order.setAddressStreet(street);
            order.setAddressBuilding(building);
            order.setAddressApartment(apartment);
        } else {
            order.setPickupAddress(pickupAddress != null
                    ? pickupAddress
                    : "г. Москва, ул. Финансовая, д. 15, офис 301");
        }

        for (CartItem ci : cartItems) {
            Loan loan = loanRepository.findByIdForUpdate(ci.getLoan().getId()).orElse(null);
            if (loan == null || !"На продаже".equals(loan.getStatus())) {
                throw new IllegalStateException(
                        "Товар «" + (loan != null ? loan.getLoanNumber() : "?") +
                        "» больше не доступен для покупки");
            }

            String itemName = "Товар";
            BigDecimal price = BigDecimal.ZERO;
            if (!loan.getCollaterals().isEmpty()) {
                Collateral c = loan.getCollaterals().get(0);
                itemName = c.getName() != null ? c.getName() : "Товар";
                price = c.getEstimatedPrice() != null ? c.getEstimatedPrice() : BigDecimal.ZERO;
            }
            order.getItems().add(new OrderItem(order, loan, itemName, price));

            loan.setStatus("Куплен");
            loanRepository.save(loan);
        }

        order.setOrderNumber(numberSequenceService.nextNumber("O", "O"));

        return orderRepository.save(order);
    }

    // смена статуса заказа сотрудником. При отмене деньги возвращаются на счет клиента,
    // а купленные вещи снова выставляются в магазин
    @Transactional
    public Order changeStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));
        String oldStatus = order.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(newStatus)) {
            throw new IllegalStateException("Нельзя перевести заказ из «" + oldStatus + "» в «" + newStatus + "»");
        }

        if ("Отменён".equals(newStatus)) {
            if (order.getClient() != null && order.getTotalAmount() != null
                    && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                accountService.deposit(order.getClient().getId(), order.getTotalAmount(),
                        "Возврат за отмененный заказ " + order.getOrderNumber());
            }
            for (OrderItem item : order.getItems()) {
                Loan loan = item.getLoan();
                if (loan != null && "Куплен".equals(loan.getStatus())) {
                    loan.setStatus("На продаже");
                    loanRepository.save(loan);
                }
            }
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
