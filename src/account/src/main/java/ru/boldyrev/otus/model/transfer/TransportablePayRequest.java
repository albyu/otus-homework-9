package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.boldyrev.otus.model.entity.PayRequest;
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

    public TransportablePayRequest(PayRequest payRequest){
        this.id = payRequest.getId();
        this.username = payRequest.getUsername();
        this.orderId = payRequest.getOrderId();
        this.amount = payRequest.getAmount();
        this.payResult = payRequest.getPayResult();
        if (payRequest.getTransaction() != null) {
            this.transactionId = payRequest.getTransaction().getId();
        }
        this.timestamp = payRequest.getTimestamp();

    }


}
