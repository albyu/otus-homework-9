package ru.boldyrev.otus.model.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.enums.TransactionType;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@NoArgsConstructor
@Data
@Accessors(chain = true)
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    double amount;

    Timestamp timestamp;

    @Enumerated(EnumType.STRING)
    TransactionType transactionType;

    String orderId;
}
