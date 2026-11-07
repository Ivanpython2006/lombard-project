package com.lombard.repository;

import com.lombard.model.Client;
import com.lombard.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

//Проверяем выборку заказов клиента
@DataJpaTest
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;
    @Autowired TestEntityManager em;

    @Test
    void findByClientId_oneOrder_returnsIt() {
        Client c = new Client(); c.setFullName("A B"); c.setPhone("+70000000002");
        em.persist(c);
        Order o = new Order(); o.setClient(c); o.setTotalAmount(BigDecimal.TEN);
        o.setDeliveryType("PICKUP");
        orderRepository.save(o);
        em.flush();

        assertThat(orderRepository.findByClientIdOrderByCreatedAtDesc(c.getId())).hasSize(1);
    }
}