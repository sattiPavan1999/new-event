package com.ticketing.orderservice.repository;

import com.ticketing.orderservice.entity.Order;
import com.ticketing.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);
}
