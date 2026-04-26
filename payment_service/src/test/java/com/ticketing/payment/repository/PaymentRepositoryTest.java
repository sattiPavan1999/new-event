package com.ticketing.payment.repository;

import com.ticketing.payment.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentEventRepository paymentEventRepository;

    private Order saveOrder() {
        Order order = new Order();
        order.setBuyerId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("1000.00"));
        return orderRepository.save(order);
    }

    private Payment savePayment(UUID orderId, String paymentId) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        return paymentRepository.save(payment);
    }

    @Test
    void findByOrderId_existingPayment_returnsPayment() {
        Order order = saveOrder();
        savePayment(order.getId(), "pay_findme");

        Optional<Payment> found = paymentRepository.findByOrderId(order.getId());

        assertTrue(found.isPresent());
        assertEquals("pay_findme", found.get().getPaymentId());
    }

    @Test
    void findByOrderId_noPayment_returnsEmpty() {
        assertFalse(paymentRepository.findByOrderId(UUID.randomUUID()).isPresent());
    }

    @Test
    void existsByOrderId_existingPayment_returnsTrue() {
        Order order = saveOrder();
        savePayment(order.getId(), "pay_exists");

        assertTrue(paymentRepository.existsByOrderId(order.getId()));
    }

    @Test
    void existsByOrderId_noPayment_returnsFalse() {
        assertFalse(paymentRepository.existsByOrderId(UUID.randomUUID()));
    }

    @Test
    void paymentEventRepository_existsByEventId_detectsDuplicate() {
        Order order = saveOrder();
        Payment payment = savePayment(order.getId(), "pay_event_123");

        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(payment.getId());
        event.setEventId("event_abc");
        event.setEventType("payment_link.paid");
        event.setPayload("{\"test\":true}");
        paymentEventRepository.save(event);

        assertTrue(paymentEventRepository.existsByEventId("event_abc"));
        assertFalse(paymentEventRepository.existsByEventId("event_unknown"));
    }

    @Test
    void orderRepository_findByPaymentLinkId_returnsOrder() {
        Order order = saveOrder();
        order.setPaymentLinkId("plink_test_link");
        orderRepository.save(order);

        Optional<Order> found = orderRepository.findByPaymentLinkId("plink_test_link");

        assertTrue(found.isPresent());
        assertEquals("plink_test_link", found.get().getPaymentLinkId());
    }

    @Test
    void save_paymentFieldsPersisted() {
        Order order = saveOrder();
        Payment payment = savePayment(order.getId(), "pay_persist");

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.SUCCEEDED, found.get().getStatus());
        assertEquals("INR", found.get().getCurrency());
        assertEquals(0, new BigDecimal("1000.00").compareTo(found.get().getAmount()));
    }
}
