package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}