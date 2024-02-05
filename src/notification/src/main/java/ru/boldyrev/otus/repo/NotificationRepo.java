package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.Notification;

import java.util.List;

public interface NotificationRepo  extends JpaRepository<Notification, Long> {
    List<Notification> findByUsername(String username);
}
