package ru.boldyrev.otus.model.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "USERS")
public class User {

    @Id
    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String password;


    @Override
    public String toString(){
      return ", username = " + username +
              ", firstName = " + firstName +
              ", lastName = " + lastName +
              ", email = " + email +
              ", phone = " + phone +
              ", password = " + password;
    }
}
