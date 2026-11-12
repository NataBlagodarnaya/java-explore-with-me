package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
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
                .created(LocalDateTime.now())
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }
}