package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.model.ParticipationRequest;
import ru.practicum.ewm.main.model.RequestStatus;

import java.util.*;
import java.util.stream.Collectors;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id as eventId, count(r) as requestCount "
            + "from ParticipationRequest r "
            + "where r.event.id in :eventIds and r.status = :status "
            + "group by r.event.id")
    List<EventRequestCount> countByEventIds(@Param("eventIds") Collection<Long> eventIds,
                                            @Param("status") RequestStatus status);

    default Map<Long, Long> countConfirmedByEventIds(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return new HashMap<>();
        }
        return countByEventIds(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(EventRequestCount::getEventId, EventRequestCount::getRequestCount));
    }

    List<ParticipationRequest> findAllByEventIdAndIdIn(Long eventId, Collection<Long> ids);
}