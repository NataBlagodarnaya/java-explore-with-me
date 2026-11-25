package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.User;

import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @Query("select u from User u order by u.id limit :size offset :from")
    List<User> findAllWithOffset(@Param("from") int from, @Param("size") int size);

    @Query("select u from User u where u.id in :ids order by u.id limit :size offset :from")
    List<User> findAllByIdsWithOffset(@Param("ids") Collection<Long> ids,
                                      @Param("from") int from,
                                      @Param("size") int size);
}