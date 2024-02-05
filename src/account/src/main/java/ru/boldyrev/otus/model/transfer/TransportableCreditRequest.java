package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransportableCreditRequest {
    String username;
    double amount;
}
