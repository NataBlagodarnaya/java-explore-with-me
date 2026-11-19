package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.dto.NewEventRequest;
import ru.practicum.ewm.main.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.main.dto.UpdateEventUserRequest;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.AdminStateAction;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventSort;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.model.UserStateAction;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventStatsService eventStatsService;

    @InjectMocks
    private EventServiceImpl eventService;


    @Test
    void addEvent_shouldSaveEvent_whenDataIsValid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(createCategory()));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });

        EventFullDto result = eventService.addEvent(1L, createNewEventRequest(LocalDateTime.now().plusDays(1)));

        assertEquals(10L, result.getId());
        assertEquals("Концерт группы", result.getTitle());
        assertEquals(EventState.PENDING, result.getState());
        assertEquals(0L, result.getConfirmedRequests());
    }

    @Test
    void addEvent_shouldThrowConflict_whenEventDateIsLessThanTwoHoursAway() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(createCategory()));
        NewEventRequest request = createNewEventRequest(LocalDateTime.now().plusHours(1));

        assertThrows(ConflictException.class, () -> eventService.addEvent(1L, request));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void addEvent_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        NewEventRequest request = createNewEventRequest(LocalDateTime.now().plusDays(1));

        assertThrows(NotFoundException.class, () -> eventService.addEvent(1L, request));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void addEvent_shouldThrowNotFound_whenCategoryDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());
        NewEventRequest request = createNewEventRequest(LocalDateTime.now().plusDays(1));

        assertThrows(NotFoundException.class, () -> eventService.addEvent(1L, request));

        verify(eventRepository, never()).save(any(Event.class));
    }


    @Test
    void getUserEvent_shouldThrowNotFound_whenEventDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.getUserEvent(1L, 5L));
    }

    @Test
    void updateUserEvent_shouldThrowConflict_whenEventIsPublished() {
        Event event = createEvent(5L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.of(event));
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");

        assertThrows(ConflictException.class, () -> eventService.updateUserEvent(1L, 5L, request));

        assertEquals("Концерт группы", event.getTitle());
    }

    @Test
    void updateUserEvent_shouldChangeOnlyPassedFields() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.of(event));
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");
        request.setParticipantLimit(20);

        EventFullDto result = eventService.updateUserEvent(1L, 5L, request);

        assertEquals("Новое название", result.getTitle());
        assertEquals(20, result.getParticipantLimit());
        assertEquals("Концерт под открытым небом", result.getAnnotation());
        assertEquals(EventState.PENDING, result.getState());
    }

    @Test
    void updateUserEvent_shouldCancelEvent_whenStateActionIsCancelReview() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.of(event));
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setStateAction(UserStateAction.CANCEL_REVIEW);

        EventFullDto result = eventService.updateUserEvent(1L, 5L, request);

        assertEquals(EventState.CANCELED, result.getState());
    }

    @Test
    void updateUserEvent_shouldThrowConflict_whenNewDateIsLessThanTwoHoursAway() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.of(event));
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setEventDate(LocalDateTime.now().plusHours(1));

        assertThrows(ConflictException.class, () -> eventService.updateUserEvent(1L, 5L, request));
    }


    @Test
    void updateEventByAdmin_shouldPublishEvent_whenEventIsPending() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);

        EventFullDto result = eventService.updateEventByAdmin(5L, request);

        assertEquals(EventState.PUBLISHED, result.getState());
        assertNotNull(result.getPublishedOn());
    }

    @Test
    void updateEventByAdmin_shouldThrowConflict_whenPublishingNotPendingEvent() {
        Event event = createEvent(5L, EventState.CANCELED, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);

        assertThrows(ConflictException.class, () -> eventService.updateEventByAdmin(5L, request));

        assertEquals(EventState.CANCELED, event.getState());
    }

    @Test
    void updateEventByAdmin_shouldThrowConflict_whenPublicationDateIsLessThanOneHourAway() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusMinutes(30));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);

        assertThrows(ConflictException.class, () -> eventService.updateEventByAdmin(5L, request));
    }

    @Test
    void updateEventByAdmin_shouldRejectEvent_whenEventIsPending() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.REJECT_EVENT);

        EventFullDto result = eventService.updateEventByAdmin(5L, request);

        assertEquals(EventState.CANCELED, result.getState());
    }

    @Test
    void updateEventByAdmin_shouldThrowConflict_whenRejectingPublishedEvent() {
        Event event = createEvent(5L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.REJECT_EVENT);

        assertThrows(ConflictException.class, () -> eventService.updateEventByAdmin(5L, request));

        assertEquals(EventState.PUBLISHED, event.getState());
    }

    @Test
    void updateEventByAdmin_shouldThrowNotFound_whenEventDoesNotExist() {
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.updateEventByAdmin(5L, new UpdateEventAdminRequest()));
    }

    @Test
    void getEventsByAdmin_shouldThrowBadRequest_whenRangeEndIsBeforeRangeStart() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        assertThrows(BadRequestException.class,
                () -> eventService.getEventsByAdmin(null, null, null, start, end, 0, 10));
    }


    @Test
    void getPublicEvents_shouldThrowBadRequest_whenRangeEndIsBeforeRangeStart() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        assertThrows(BadRequestException.class,
                () -> eventService.getPublicEvents(null, null, null, start, end, false, null, 0, 10));
    }

    @Test
    void getPublicEvents_shouldReturnOnlyPublishedEventsMatchingTextIgnoringCase() {
        Event matching = createEvent(1L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        Event otherText = createEvent(2L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        otherText.setAnnotation("Выставка современного искусства");
        otherText.setDescription("Работы молодых художников");
        Event notPublished = createEvent(3L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(eventRepository.findAll()).thenReturn(List.of(matching, otherText, notPublished));
        when(eventStatsService.getViews(any())).thenReturn(Map.of(1L, 7L));

        List<EventShortDto> result = eventService.getPublicEvents("КОНЦЕРТ", null, null, null, null,
                false, null, 0, 10);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(7L, result.get(0).getViews());
    }

    @Test
    void getPublicEvents_shouldSortByViewsDescending_whenSortIsViews() {
        Event first = createEvent(1L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        Event second = createEvent(2L, EventState.PUBLISHED, LocalDateTime.now().plusDays(3));
        when(eventRepository.findAll()).thenReturn(List.of(first, second));
        when(eventStatsService.getViews(any())).thenReturn(Map.of(1L, 1L, 2L, 5L));

        List<EventShortDto> result = eventService.getPublicEvents(null, null, null, null, null,
                false, EventSort.VIEWS, 0, 10);

        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    void getPublicEvents_shouldSortByEventDate_whenSortIsNotPassed() {
        Event later = createEvent(1L, EventState.PUBLISHED, LocalDateTime.now().plusDays(5));
        Event earlier = createEvent(2L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        when(eventRepository.findAll()).thenReturn(List.of(later, earlier));
        when(eventStatsService.getViews(any())).thenReturn(Map.of());

        List<EventShortDto> result = eventService.getPublicEvents(null, null, null, null, null,
                false, null, 0, 10);

        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    void getPublicEvent_shouldThrowNotFound_whenEventIsNotPublished() {
        Event event = createEvent(5L, EventState.PENDING, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(NotFoundException.class, () -> eventService.getPublicEvent(5L));
    }

    @Test
    void getPublicEvent_shouldReturnEventWithViews_whenEventIsPublished() {
        Event event = createEvent(5L, EventState.PUBLISHED, LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventStatsService.getViews(any())).thenReturn(Map.of(5L, 3L));

        EventFullDto result = eventService.getPublicEvent(5L);

        assertEquals(5L, result.getId());
        assertEquals(3L, result.getViews());
    }


    private User createUser() {
        return User.builder().id(1L).name("Иван Петров").email("ivan@practicummail.ru").build();
    }

    private Category createCategory() {
        return Category.builder().id(2L).name("Концерты").build();
    }

    private Event createEvent(Long id, EventState state, LocalDateTime eventDate) {
        return Event.builder()
                .id(id)
                .title("Концерт группы")
                .annotation("Концерт под открытым небом")
                .description("Большой концерт для всех желающих")
                .category(createCategory())
                .initiator(createUser())
                .eventDate(eventDate)
                .createdOn(LocalDateTime.now().minusDays(1))
                .location(new Location(55.75f, 37.62f))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .build();
    }

    private NewEventRequest createNewEventRequest(LocalDateTime eventDate) {
        NewEventRequest request = new NewEventRequest();
        request.setAnnotation("Концерт под открытым небом");
        request.setCategory(2L);
        request.setDescription("Большой концерт для всех желающих");
        request.setEventDate(eventDate);
        request.setLocation(new LocationDto(55.75f, 37.62f));
        request.setPaid(false);
        request.setParticipantLimit(10);
        request.setRequestModeration(true);
        request.setTitle("Концерт группы");
        return request;
    }
}