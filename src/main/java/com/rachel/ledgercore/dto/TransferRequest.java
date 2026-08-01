package com.rachel.ledgercore.dto;


import java.math.BigDecimal;

public class TransferRequest {

    String fromAccountNumber;

    String toAccountNumber;

    BigDecimal amount;
}
