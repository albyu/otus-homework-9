package ru.boldyrev.otus.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.boldyrev.otus.model.entity.Account;

@Data
@NoArgsConstructor
public class TransportableAccount {
    String userName;
    double balance;

    public TransportableAccount(Account account){
        this.userName = account.getUserName();
        this.balance = account.getBalance();
    }



}
