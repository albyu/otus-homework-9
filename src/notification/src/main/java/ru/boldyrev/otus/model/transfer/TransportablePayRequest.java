package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.boldyrev.otus.model.enums.PayResult;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class TransportablePayRequest {
    Long id;

    String username;

    String orderId;

    double amount;

    PayResult payResult;

    Long transactionId;

    Timestamp timestamp;

}