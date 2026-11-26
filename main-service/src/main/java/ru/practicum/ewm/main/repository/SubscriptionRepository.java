package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Subscription;
import ru.practicum.ewm.main.model.User;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriberIdAndAuthorId(Long subscriberId, Long authorId);

    Optional<Subscription> findBySubscriberIdAndAuthorId(Long subscriberId, Long authorId);

    @Query("select s.author from Subscription s where s.subscriber.id = :userId order by s.author.id")
    List<User> findAuthorsBySubscriberId(@Param("userId") Long userId);
}