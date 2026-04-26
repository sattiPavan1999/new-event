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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order saveOrder(UUID buyerId, OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setBuyerId(buyerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("2500.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Test
    void findByBuyerIdAndStatus_returnsOnlyMatchingOrders() {
        UUID buyerId = UUID.randomUUID();
        saveOrder(buyerId, OrderStatus.CONFIRMED);
        saveOrder(buyerId, OrderStatus.CONFIRMED);
        saveOrder(buyerId, OrderStatus.PENDING);
        saveOrder(UUID.randomUUID(), OrderStatus.CONFIRMED);

        Page<Order> page = orderRepository.findByBuyerIdAndStatus(
                buyerId, OrderStatus.CONFIRMED, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(o -> o.getBuyerId().equals(buyerId)
                        && o.getStatus() == OrderStatus.CONFIRMED));
    }

    @Test
    void findByIdAndBuyerId_correctOwner_returnsOrder() {
        UUID buyerId = UUID.randomUUID();
        Order saved = saveOrder(buyerId, OrderStatus.CONFIRMED);

        Optional<Order> found = orderRepository.findByIdAndBuyerId(saved.getId(), buyerId);

        assertTrue(found.isPresent());
        assertEquals(buyerId, found.get().getBuyerId());
    }

    @Test
    void findByIdAndBuyerId_wrongOwner_returnsEmpty() {
        UUID buyerId = UUID.randomUUID();
        Order saved = saveOrder(buyerId, OrderStatus.CONFIRMED);

        Optional<Order> found = orderRepository.findByIdAndBuyerId(saved.getId(), UUID.randomUUID());

        assertFalse(found.isPresent());
    }

    @Test
    void findByRazorpayPaymentLinkId_returnsMatchingOrder() {
        UUID buyerId = UUID.randomUUID();
        Order order = saveOrder(buyerId, OrderStatus.PENDING);
        order.setRazorpayPaymentLinkId("rpl_123abc");
        orderRepository.save(order);

        Optional<Order> found = orderRepository.findByRazorpayPaymentLinkId("rpl_123abc");

        assertTrue(found.isPresent());
        assertEquals("rpl_123abc", found.get().getRazorpayPaymentLinkId());
    }

    @Test
    void findByRazorpayPaymentLinkId_noMatch_returnsEmpty() {
        Optional<Order> found = orderRepository.findByRazorpayPaymentLinkId("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_existingOrder_returnsTrue() {
        Order saved = saveOrder(UUID.randomUUID(), OrderStatus.PENDING);
        assertTrue(orderRepository.existsById(saved.getId()));
    }

    @Test
    void existsById_missingOrder_returnsFalse() {
        assertFalse(orderRepository.existsById(UUID.randomUUID()));
    }
}
