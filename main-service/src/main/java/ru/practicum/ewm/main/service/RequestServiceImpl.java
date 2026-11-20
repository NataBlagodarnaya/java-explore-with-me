package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateDto;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.RequestMapper;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.ParticipationRequest;
import ru.practicum.ewm.main.model.RequestStatus;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.main.model.RequestUpdateStatus;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        findUser(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getId))
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = findUser(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists: user id=" + userId
                    + ", event id=" + eventId);
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add a participation request to own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in an unpublished event");
        }
        if (event.getParticipantLimit() > 0
                && requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        boolean autoConfirm = !event.isRequestModeration() || event.getParticipantLimit() == 0;
        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .event(event)
                .requester(user)
                .status(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создана заявка id={} пользователя id={} на событие id={}", saved.getId(), userId, eventId);
        return RequestMapper.toParticipationRequestDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        findUser(userId);
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        request.setStatus(RequestStatus.CANCELED);
        log.info("Отменена заявка id={} пользователем id={}", requestId, userId);
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        findUser(userId);
        findUserEvent(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getId))
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateDto updateRequestStatus(Long userId, Long eventId,
                                                           EventRequestStatusUpdateRequest request) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (event.getParticipantLimit() == 0 || !event.isRequestModeration()) {
            return new EventRequestStatusUpdateDto(confirmed, rejected);
        }

        List<ParticipationRequest> requests = findEventRequests(eventId, request.getRequestIds());
        for (ParticipationRequest participation : requests) {
            if (participation.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        for (ParticipationRequest participation : requests) {
            if (request.getStatus() == RequestUpdateStatus.CONFIRMED) {
                if (confirmedCount >= event.getParticipantLimit()) {
                    throw new ConflictException("The participant limit has been reached");
                }
                participation.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmed.add(RequestMapper.toParticipationRequestDto(participation));
            } else {
                participation.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(participation));
            }
        }

        if (confirmedCount >= event.getParticipantLimit()) {
            for (ParticipationRequest pending
                    : requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING)) {
                pending.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(pending));
            }
        }

        log.info("Изменены статусы заявок на событие id={} пользователем id={}", eventId, userId);
        return new EventRequestStatusUpdateDto(confirmed, rejected);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event findUserEvent(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private List<ParticipationRequest> findEventRequests(Long eventId, List<Long> requestIds) {
        return requestIds.stream()
                .distinct()
                .map(id -> requestRepository.findByIdAndEventId(id, eventId)
                        .orElseThrow(() -> new NotFoundException("Request with id=" + id + " was not found")))
                .sorted(Comparator.comparing(ParticipationRequest::getId))
                .toList();
    }
}