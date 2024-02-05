package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.Notification;
import ru.boldyrev.otus.model.enums.PayResult;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TransportableNotification {

    Long id;

    String email;

    String orderId;

    double amount;

    PayResult payResult;

    Timestamp timestamp;

    String text;

    Boolean isDeadNotification;

    public TransportableNotification(Notification note){
        this.id = note.getId();
        this.email = note.getEmail();
        this.orderId = note.getOrderId();
        this.amount = note.getAmount();
        this.payResult = note.getPayResult();
        this.timestamp = note.getTimestamp();
        this.text = note.getText();
        this.isDeadNotification = note.getIsDeadNotification();
    }

}
