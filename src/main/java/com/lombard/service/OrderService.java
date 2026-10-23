package com.lombard.service;

import com.lombard.model.*;
import com.lombard.repository.LoanRepository;
import com.lombard.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final LoanRepository loanRepository;
    private final NumberSequenceService numberSequenceService;

    public OrderService(OrderRepository orderRepository,
                        LoanRepository loanRepository,
                        NumberSequenceService numberSequenceService) {
        this.orderRepository = orderRepository;
        this.loanRepository = loanRepository;
        this.numberSequenceService = numberSequenceService;
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
            order.setOrderNumber(numberSequenceService.nextNumber("O", "M"));
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
            Loan loan = loanRepository.findById(ci.getLoan().getId()).orElse(null);
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

        order.setOrderNumber(numberSequenceService.nextNumber("O", "M"));

        return orderRepository.save(order);
    }
}