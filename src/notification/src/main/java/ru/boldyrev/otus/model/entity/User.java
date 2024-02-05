package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "USERS")
public class User {
    @Id
    String username;

    String email;

}
