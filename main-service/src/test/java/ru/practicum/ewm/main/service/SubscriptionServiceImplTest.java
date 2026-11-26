package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.Category;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventStatsService eventStatsService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void subscribe_shouldSaveSubscriptionAndReturnAuthor() {
        User subscriber = createUser(1L, "Иван Петров", "ivan@practicummail.ru");
        User author = createUser(2L, "Мария Сидорова", "maria@practicummail.ru");
        when(userRepository.findById(1L)).thenReturn(Optional.of(subscriber));
        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(subscriptionRepository.existsBySubscriberIdAndAuthorId(1L, 2L)).thenReturn(false);

        UserShortDto result = subscriptionService.subscribe(1L, 2L);

        assertEquals(2L, result.getId());
        assertEquals("Мария Сидорова", result.getName());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void subscribe_shouldThrowConflict_whenUserSubscribesToHimself() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));

        assertThrows(ConflictException.class, () -> subscriptionService.subscribe(1L, 1L));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void subscribe_shouldThrowConflict_whenSubscriptionAlreadyExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(createUser(2L, "Мария Сидорова",
                "maria@practicummail.ru")));
        when(subscriptionRepository.existsBySubscriberIdAndAuthorId(1L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> subscriptionService.subscribe(1L, 2L));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void subscribe_shouldThrowNotFound_whenSubscriberDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subscriptionService.subscribe(1L, 2L));
    }

    @Test
    void subscribe_shouldThrowNotFound_whenAuthorDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subscriptionService.subscribe(1L, 2L));
    }

    @Test
    void unsubscribe_shouldDeleteSubscription() {
        Subscription subscription = Subscription.builder().id(5L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(subscriptionRepository.findBySubscriberIdAndAuthorId(1L, 2L))
                .thenReturn(Optional.of(subscription));

        subscriptionService.unsubscribe(1L, 2L);

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void unsubscribe_shouldThrowNotFound_whenSubscriptionDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(subscriptionRepository.findBySubscriberIdAndAuthorId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subscriptionService.unsubscribe(1L, 2L));

        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    void getSubscriptions_shouldReturnAuthorsAsShortDtos() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(subscriptionRepository.findAuthorsBySubscriberId(1L)).thenReturn(List.of(
                createUser(2L, "Мария Сидорова", "maria@practicummail.ru"),
                createUser(3L, "Пётр Иванов", "petr@practicummail.ru")));

        List<UserShortDto> result = subscriptionService.getSubscriptions(1L);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Пётр Иванов", result.get(1).getName());
    }

    @Test
    void getSubscriptions_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subscriptionService.getSubscriptions(1L));
    }

    @Test
    void getSubscriptionEvents_shouldReturnEventsWithConfirmedAndViews() {
        Event event = createEvent(10L, createUser(2L, "Мария Сидорова", "maria@practicummail.ru"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(eventRepository.findSubscriptionEvents(eq(1L), eq(EventState.PUBLISHED), any(LocalDateTime.class),
                eq(0), eq(10))).thenReturn(List.of(event));
        when(requestRepository.countConfirmedByEventIds(any())).thenReturn(Map.of(10L, 2L));
        when(eventStatsService.getViews(any())).thenReturn(Map.of(10L, 7L));

        List<EventShortDto> result = subscriptionService.getSubscriptionEvents(1L, 0, 10);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(2L, result.get(0).getConfirmedRequests());
        assertEquals(7L, result.get(0).getViews());
    }

    @Test
    void getSubscriptionEvents_shouldPassFromAndSizeToRepository() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "Иван Петров",
                "ivan@practicummail.ru")));
        when(eventRepository.findSubscriptionEvents(eq(1L), eq(EventState.PUBLISHED), any(LocalDateTime.class),
                anyInt(), anyInt())).thenReturn(List.of());

        List<EventShortDto> result = subscriptionService.getSubscriptionEvents(1L, 5, 3);

        assertTrue(result.isEmpty());
        verify(eventRepository).findSubscriptionEvents(eq(1L), eq(EventState.PUBLISHED),
                any(LocalDateTime.class), eq(5), eq(3));
    }

    @Test
    void getSubscriptionEvents_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subscriptionService.getSubscriptionEvents(1L, 0, 10));
    }

    private User createUser(Long id, String name, String email) {
        return User.builder().id(id).name(name).email(email).build();
    }

    private Event createEvent(Long id, User initiator) {
        return Event.builder()
                .id(id)
                .title("Концерт группы")
                .annotation("Живой концерт под открытым небом")
                .category(Category.builder().id(1L).name("Концерты").build())
                .initiator(initiator)
                .eventDate(LocalDateTime.now().plusDays(3))
                .state(EventState.PUBLISHED)
                .build();
    }
}