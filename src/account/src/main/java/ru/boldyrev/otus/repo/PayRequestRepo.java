package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.PayRequest;
import ru.boldyrev.otus.model.entity.Transaction;

import java.util.List;

public interface PayRequestRepo  extends JpaRepository<PayRequest, Long> {
    List<PayRequest> findByUsername(String username);
}
