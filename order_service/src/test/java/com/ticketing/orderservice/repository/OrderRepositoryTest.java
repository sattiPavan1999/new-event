package com.ticketing.orderservice.repository;

import com.ticketing.orderservice.entity.Order;
import com.ticketing.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order saveOrder(Long buyerId, OrderStatus status) {
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("2500.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Test
    void findByBuyerIdAndStatus_returnsOnlyMatchingOrders() {
        Long buyerId = 1L;
        saveOrder(buyerId, OrderStatus.CONFIRMED);
        saveOrder(buyerId, OrderStatus.CONFIRMED);
        saveOrder(buyerId, OrderStatus.PENDING);
        saveOrder(2L, OrderStatus.CONFIRMED);

        Page<Order> page = orderRepository.findByBuyerIdAndStatus(
                buyerId, OrderStatus.CONFIRMED, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(o -> o.getBuyerId().equals(buyerId)
                        && o.getStatus() == OrderStatus.CONFIRMED));
    }

    @Test
    void findById_existingOrder_returnsOrder() {
        Long buyerId = 1L;
        Order saved = saveOrder(buyerId, OrderStatus.CONFIRMED);

        Optional<Order> found = orderRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(buyerId, found.get().getBuyerId());
    }

    @Test
    void findById_missingOrder_returnsEmpty() {
        Optional<Order> found = orderRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_existingOrder_returnsTrue() {
        Order saved = saveOrder(1L, OrderStatus.PENDING);
        assertTrue(orderRepository.existsById(saved.getId()));
    }

    @Test
    void existsById_missingOrder_returnsFalse() {
        assertFalse(orderRepository.existsById(999L));
    }
}
