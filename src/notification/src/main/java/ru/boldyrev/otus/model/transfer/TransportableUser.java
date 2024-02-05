package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TransportableUser {
    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String password;
}
