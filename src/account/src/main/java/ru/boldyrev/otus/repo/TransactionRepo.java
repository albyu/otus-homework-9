package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.Transaction;

import java.util.List;


public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

}
