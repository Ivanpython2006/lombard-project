package com.lombard.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Тесты заказа: подписи способа получения и сборка адреса для страницы заказа
class OrderTest {

    @Test
    void deliveryTypeLabel_pickupAndDelivery_returnsRussianText() {
        Order order = new Order();

        order.setDeliveryType("PICKUP");
        assertEquals("Самовывоз", order.getDeliveryTypeLabel());

        order.setDeliveryType("DELIVERY");
        assertEquals("Доставка", order.getDeliveryTypeLabel());
    }

    @Test
    void fullAddress_pickup_returnsPickupAddress() {
        Order order = new Order();
        order.setDeliveryType("PICKUP");
        order.setPickupAddress("Офис 1");
        assertEquals("Офис 1", order.getFullAddress());

        order.setPickupAddress(null);
        assertEquals("—", order.getFullAddress());
    }

    // адрес доставки собирается из города улицы дома и квартиры
    @Test
    void fullAddress_delivery_buildsAddressString() {
        Order order = new Order();
        order.setDeliveryType("DELIVERY");
        order.setAddressCity("Москва");
        order.setAddressStreet("Ленина");
        order.setAddressBuilding("1");
        order.setAddressApartment("2");

        assertEquals("г. Москва, ул. Ленина, д. 1, кв. 2", order.getFullAddress());
    }

    @Test
    void fullAddress_deliveryWithoutAddress_returnsDash() {
        Order order = new Order();
        order.setDeliveryType("DELIVERY");

        assertEquals("—", order.getFullAddress());
    }

    // перед сохранением заказ получает дату статус оплачен и временный номер
    @Test
    void prePersist_newOrder_setsDefaults() {
        Order o = new Order();

        o.prePersist();

        assertNotNull(o.getCreatedAt());
        assertEquals("Оплачен", o.getStatus());
        assertEquals("O_UNKNOWN", o.getOrderNumber());
    }
}
