package com.ticketing.orderservice.exception;

import java.math.BigDecimal;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(BigDecimal required) {
        super("Insufficient wallet balance. Required: ₹" + required);
    }
}
