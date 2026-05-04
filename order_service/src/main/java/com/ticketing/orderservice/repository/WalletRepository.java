package com.ticketing.orderservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class WalletRepository {

    private final JdbcTemplate jdbcTemplate;

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int debitWallet(Long userId, BigDecimal amount) {
        String sql = "UPDATE auth.users SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?";
        return jdbcTemplate.update(sql, amount, userId, amount);
    }

    public int creditWallet(Long userId, BigDecimal amount) {
        String sql = "UPDATE auth.users SET wallet_balance = wallet_balance + ? WHERE id = ?";
        return jdbcTemplate.update(sql, amount, userId);
    }

    public BigDecimal getBalance(Long userId) {
        String sql = "SELECT wallet_balance FROM auth.users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }
}
