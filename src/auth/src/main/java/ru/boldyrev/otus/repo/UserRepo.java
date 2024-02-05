package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.User;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, String> {
    Optional<User> findByUsernameAndPassword(String username, String password);
}
