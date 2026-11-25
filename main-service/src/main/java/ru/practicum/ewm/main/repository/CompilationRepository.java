package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    @Query("select c.id from Compilation c "
            + "where (:ignorePinned = true or c.pinned = :pinned) "
            + "order by c.id limit :size offset :from")
    List<Long> findIdsWithOffset(@Param("ignorePinned") boolean ignorePinned,
                                 @Param("pinned") boolean pinned,
                                 @Param("from") int from,
                                 @Param("size") int size);

    @Query("select distinct c from Compilation c "
            + "left join fetch c.events e "
            + "left join fetch e.category "
            + "left join fetch e.initiator "
            + "where c.id in :ids order by c.id")
    List<Compilation> findAllWithEventsByIds(@Param("ids") Collection<Long> ids);

    @Query("select distinct c from Compilation c "
            + "left join fetch c.events e "
            + "left join fetch e.category "
            + "left join fetch e.initiator "
            + "where c.id = :id")
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);
}