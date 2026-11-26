package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    String PUBLIC_FILTERS = "from Event e join fetch e.category join fetch e.initiator "
            + "where e.state = :state "
            + "and (:allText = true or lower(e.annotation) like :pattern "
            + "or lower(e.description) like :pattern) "
            + "and (:allCategories = true or e.category.id in :categories) "
            + "and (:allPaid = true or e.paid = :paid) "
            + "and e.eventDate >= :rangeStart and e.eventDate <= :rangeEnd "
            + "and (:onlyAvailable = false or e.participantLimit = 0 or e.participantLimit > "
            + "(select count(r) from ParticipationRequest r "
            + "where r.event = e and r.status = :confirmed)) ";

    boolean existsByCategoryId(Long categoryId);

    @Query("select e from Event e join fetch e.category join fetch e.initiator "
            + "where e.id = :id and e.initiator.id = :initiatorId")
    Optional<Event> findByIdAndInitiatorId(@Param("id") Long id, @Param("initiatorId") Long initiatorId);

    @Query("select e from Event e join fetch e.category join fetch e.initiator "
            + "where (:allUsers = true or e.initiator.id in :users) "
            + "and (:allStates = true or e.state in :states) "
            + "and (:allCategories = true or e.category.id in :categories) "
            + "and e.eventDate >= :rangeStart and e.eventDate <= :rangeEnd "
            + "order by e.id "
            + "limit :size offset :from")
    List<Event> findAllByAdminFilters(@Param("allUsers") boolean allUsers,
                                      @Param("users") Collection<Long> users,
                                      @Param("allStates") boolean allStates,
                                      @Param("states") Collection<EventState> states,
                                      @Param("allCategories") boolean allCategories,
                                      @Param("categories") Collection<Long> categories,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      @Param("from") int from,
                                      @Param("size") int size);

    @Query("select e " + PUBLIC_FILTERS + "order by e.eventDate")
    List<Event> findPublicEvents(@Param("state") EventState state,
                                 @Param("allText") boolean allText,
                                 @Param("pattern") String pattern,
                                 @Param("allCategories") boolean allCategories,
                                 @Param("categories") Collection<Long> categories,
                                 @Param("allPaid") boolean allPaid,
                                 @Param("paid") boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("onlyAvailable") boolean onlyAvailable,
                                 @Param("confirmed") RequestStatus confirmed);

    @Query("select e " + PUBLIC_FILTERS + "order by e.eventDate limit :size offset :from")
    List<Event> findPublicEventsPage(@Param("state") EventState state,
                                     @Param("allText") boolean allText,
                                     @Param("pattern") String pattern,
                                     @Param("allCategories") boolean allCategories,
                                     @Param("categories") Collection<Long> categories,
                                     @Param("allPaid") boolean allPaid,
                                     @Param("paid") boolean paid,
                                     @Param("rangeStart") LocalDateTime rangeStart,
                                     @Param("rangeEnd") LocalDateTime rangeEnd,
                                     @Param("onlyAvailable") boolean onlyAvailable,
                                     @Param("confirmed") RequestStatus confirmed,
                                     @Param("from") int from,
                                     @Param("size") int size);

    @Query("select e from Event e join fetch e.category join fetch e.initiator "
            + "where e.initiator.id = :initiatorId order by e.id limit :size offset :from")
    List<Event> findAllByInitiatorIdWithOffset(@Param("initiatorId") Long initiatorId,
                                               @Param("from") int from,
                                               @Param("size") int size);

    @Query("select e from Event e join fetch e.category join fetch e.initiator "
            + "where e.id in :ids")
    List<Event> findAllByIdsWithDetails(@Param("ids") Collection<Long> ids);

    @Query("select e from Event e join fetch e.category join fetch e.initiator where e.id = :id")
    Optional<Event> findByIdWithDetails(@Param("id") Long id);

    @Query("select e from Event e join fetch e.category join fetch e.initiator i "
            + "where e.initiator.id in (select s.author.id from Subscription s where s.subscriber.id = :userId) "
            + "and e.state = :state and e.eventDate > :now "
            + "order by i.name, i.id, e.eventDate, e.id "
            + "limit :size offset :from")
    List<Event> findSubscriptionEvents(@Param("userId") Long userId,
                                       @Param("state") EventState state,
                                       @Param("now") LocalDateTime now,
                                       @Param("from") int from,
                                       @Param("size") int size);
}