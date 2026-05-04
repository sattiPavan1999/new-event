package com.ticketing.orderservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTierRepository {

    private final JdbcTemplate jdbcTemplate;

    public TicketTierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int decrementRemainingQty(Long tierId, Integer quantity) {
        String sql = "UPDATE events.ticket_tiers " +
                     "SET remaining_qty = remaining_qty - ? " +
                     "WHERE id = ? AND remaining_qty >= ?";

        return jdbcTemplate.update(sql, quantity, tierId, quantity);
    }

    public int incrementRemainingQty(Long tierId, Integer quantity) {
        String sql = "UPDATE events.ticket_tiers " +
                     "SET remaining_qty = remaining_qty + ? " +
                     "WHERE id = ? AND remaining_qty + ? <= total_qty";
        return jdbcTemplate.update(sql, quantity, tierId, quantity);
    }
}
