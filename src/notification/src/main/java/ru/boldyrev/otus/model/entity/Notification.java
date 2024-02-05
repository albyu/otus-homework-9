package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.enums.PayResult;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "NOTIFICATIONS")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String username;

    String email;

    String orderId;

    double amount;

    PayResult payResult;

    Timestamp timestamp;

    String text;

    Boolean isDeadNotification;
}
