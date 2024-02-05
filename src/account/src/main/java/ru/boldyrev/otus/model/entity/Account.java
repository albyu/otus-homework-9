package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Data
@Accessors(chain = true)
@Table(name = "account", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "username", unique = true)
    private String userName;

    double balance;
}
