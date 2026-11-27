package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.mapper.UserMapper;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.Subscription;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.SubscriptionRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventStatsService eventStatsService;

    @Override
    public UserShortDto subscribe(Long userId, Long authorId) {
        User subscriber = findUser(userId);
        if (userId.equals(authorId)) {
            throw new ConflictException("User cannot subscribe to himself");
        }
        User author = findUser(authorId);
        if (subscriptionRepository.existsBySubscriberIdAndAuthorId(userId, authorId)) {
            throw new ConflictException("Subscription of user with id=" + userId
                    + " to user with id=" + authorId + " already exists");
        }
        subscriptionRepository.save(Subscription.builder()
                .subscriber(subscriber)
                .author(author)
                .build());
        log.info("Создана подписка пользователя {} на автора {}", userId, authorId);
        return UserMapper.toUserShortDto(author);
    }

    @Override
    public void unsubscribe(Long userId, Long authorId) {
        findUser(userId);
        Subscription subscription = subscriptionRepository.findBySubscriberIdAndAuthorId(userId, authorId)
                .orElseThrow(() -> new NotFoundException("Subscription of user with id=" + userId
                        + " to user with id=" + authorId + " was not found"));
        subscriptionRepository.delete(subscription);
        log.info("Удалена подписка пользователя {} на автора {}", userId, authorId);
    }

    @Override
    public List<UserShortDto> getSubscriptions(Long userId) {
        findUser(userId);
        return subscriptionRepository.findAuthorsBySubscriberId(userId).stream()
                .map(UserMapper::toUserShortDto)
                .toList();
    }

    @Override
    public List<EventShortDto> getSubscriptionEvents(Long userId, int from, int size) {
        findUser(userId);
        List<Event> events = eventRepository.findSubscriptionEvents(userId, EventState.PUBLISHED,
                LocalDateTime.now(), from, size);
        Map<Long, Long> confirmed = requestRepository.countConfirmedByEventIds(
                events.stream().map(Event::getId).toList());
        Map<Long, Long> views = eventStatsService.getViews(events);
        Map<Long, User> initiators = userRepository.findAllById(
                        events.stream().map(event -> event.getInitiator().getId()).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return events.stream()
                .map(event -> EventMapper.toEventShortDto(event,
                        initiators.get(event.getInitiator().getId()),
                        confirmed.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }
}