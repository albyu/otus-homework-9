package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.boldyrev.otus.model.entity.Transaction;
import ru.boldyrev.otus.model.enums.TransactionType;

import java.sql.Timestamp;

@NoArgsConstructor
@Data
public class TransportableTransaction {
    Long id;
    Long accountId;
    double amount;
    Timestamp timestamp;
    TransactionType transactionType;
    String orderId;
    public TransportableTransaction(Transaction transaction){
        this.id = transaction.getId();
        this.accountId = transaction.getAccount().getId();
        this.amount = transaction.getAmount();
        this.timestamp = transaction.getTimestamp();
        this.transactionType = transaction.getTransactionType();
        this.orderId = transaction.getOrderId();
    }
}
