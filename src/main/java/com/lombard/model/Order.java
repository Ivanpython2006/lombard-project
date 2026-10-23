package com.lombard.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, length = 10)
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status = "Оплачен";

    @Column(name = "delivery_type", nullable = false, length = 20)
    private String deliveryType;

    @Column(name = "address_city", length = 100)
    private String addressCity;

    @Column(name = "address_street", length = 200)
    private String addressStreet;

    @Column(name = "address_building", length = 20)
    private String addressBuilding;

    @Column(name = "address_apartment", length = 20)
    private String addressApartment;

    @Column(name = "pickup_address", length = 300)
    private String pickupAddress;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "Оплачен";
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = "O_UNKNOWN";
        }
    }

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }

    public String getAddressStreet() { return addressStreet; }
    public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }

    public String getAddressBuilding() { return addressBuilding; }
    public void setAddressBuilding(String addressBuilding) { this.addressBuilding = addressBuilding; }

    public String getAddressApartment() { return addressApartment; }
    public void setAddressApartment(String addressApartment) { this.addressApartment = addressApartment; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getDeliveryTypeLabel() {
        return "PICKUP".equals(deliveryType) ? "Самовывоз" : "Доставка";
    }

    public String getFullAddress() {
        if ("PICKUP".equals(deliveryType)) {
            return pickupAddress != null ? pickupAddress : "—";
        }
        StringBuilder sb = new StringBuilder();
        if (addressCity != null && !addressCity.isBlank()) sb.append("г. ").append(addressCity).append(", ");
        if (addressStreet != null && !addressStreet.isBlank()) sb.append("ул. ").append(addressStreet);
        if (addressBuilding != null && !addressBuilding.isBlank()) sb.append(", д. ").append(addressBuilding);
        if (addressApartment != null && !addressApartment.isBlank()) sb.append(", кв. ").append(addressApartment);
        return sb.length() > 0 ? sb.toString() : "—";
    }
}