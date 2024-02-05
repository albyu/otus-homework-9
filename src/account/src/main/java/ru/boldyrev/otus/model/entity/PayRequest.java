package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.enums.PayResult;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@NoArgsConstructor
@Data
@Accessors(chain = true)
@Table(name = "pay_request")
public class PayRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String username;

    String orderId;

    double amount;

    @Enumerated(EnumType.STRING)
    PayResult payResult;

    @OneToOne
    Transaction transaction;

    Timestamp timestamp;
}
