package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.repository.CategoryRepository;
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
public class EventServiceImpl implements EventService {

    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private static final int MIN_HOURS_BEFORE_PUBLICATION = 1;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventRequest request) {
        User user = findUser(userId);
        Category category = findCategory(request.getCategory());
        checkEventDate(request.getEventDate());

        Event saved = eventRepository.save(EventMapper.toEvent(request, category, user));
        log.info("Создано событие id={} пользователем id={}", saved.getId(), userId);
        return EventMapper.toEventFullDto(saved, countConfirmed(saved.getId()), 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        findUser(userId);
        return eventRepository.findAllByInitiatorId(userId).stream()
                .sorted(Comparator.comparing(Event::getId))
                .skip(from)
                .limit(size)
                .map(event -> EventMapper.toEventShortDto(event, countConfirmed(event.getId()), 0L))
                .toList();
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);
        return EventMapper.toEventFullDto(event, countConfirmed(eventId), 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null) {
            checkEventDate(request.getEventDate());
            event.setEventDate(request.getEventDate());
        }
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(findCategory(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(request.getLocation()));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (request.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        log.info("Обновлено событие id={} пользователем id={}", eventId, userId);
        return EventMapper.toEventFullDto(event, countConfirmed(eventId), 0L);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, int from, int size) {
        return eventRepository.findAll().stream()
                .filter(event -> users == null || users.isEmpty()
                        || users.contains(event.getInitiator().getId()))
                .filter(event -> states == null || states.isEmpty()
                        || states.contains(event.getState()))
                .filter(event -> categories == null || categories.isEmpty()
                        || categories.contains(event.getCategory().getId()))
                .filter(event -> rangeStart == null || !event.getEventDate().isBefore(rangeStart))
                .filter(event -> rangeEnd == null || !event.getEventDate().isAfter(rangeEnd))
                .sorted(Comparator.comparing(Event::getId))
                .skip(from)
                .limit(size)
                .map(event -> EventMapper.toEventFullDto(event, countConfirmed(event.getId()), 0L))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (request.getEventDate() != null) {
            checkPublicationDate(request.getEventDate());
            event.setEventDate(request.getEventDate());
        }
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(findCategory(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(request.getLocation()));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
            }
            checkPublicationDate(event.getEventDate());
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's not in the right state: "
                        + event.getState());
            }
            event.setState(EventState.CANCELED);
        }

        log.info("Обновлено событие id={} администратором", eventId);
        return EventMapper.toEventFullDto(event, countConfirmed(eventId), 0L);
    }

    private void checkPublicationDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLICATION))) {
            throw new ConflictException("Event date must be at least " + MIN_HOURS_BEFORE_PUBLICATION
                    + " hour from now. Value: " + eventDate);
        }
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ConflictException("Field: eventDate. Error: должно содержать дату, "
                    + "которая еще не наступила. Value: " + eventDate);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category findCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private Event findUserEvent(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private long countConfirmed(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }
}