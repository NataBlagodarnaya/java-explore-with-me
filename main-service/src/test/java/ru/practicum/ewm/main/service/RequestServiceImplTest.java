package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateDto;
import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.eventrequest.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.ParticipationRequest;
import ru.practicum.ewm.main.model.RequestStatus;
import ru.practicum.ewm.main.model.RequestUpdateStatus;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void addRequest_shouldCreatePendingRequest_whenModerationIsEnabled() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(0L);
        mockSave();

        ParticipationRequestDto result = requestService.addRequest(2L, 5L);

        assertEquals(100L, result.getId());
        assertEquals(5L, result.getEvent());
        assertEquals(2L, result.getRequester());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void addRequest_shouldConfirmAutomatically_whenModerationIsDisabled() {
        Event event = createEvent(EventState.PUBLISHED, 10, false);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(0L);
        mockSave();

        ParticipationRequestDto result = requestService.addRequest(2L, 5L);

        assertEquals("CONFIRMED", result.getStatus());
    }

    @Test
    void addRequest_shouldConfirmAutomatically_whenParticipantLimitIsZero() {
        Event event = createEvent(EventState.PUBLISHED, 0, true);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(false);
        mockSave();

        ParticipationRequestDto result = requestService.addRequest(2L, 5L);

        assertEquals("CONFIRMED", result.getStatus());
    }

    @Test
    void addRequest_shouldThrowConflict_whenRequestAlreadyExists() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> requestService.addRequest(2L, 5L));

        verify(requestRepository, never()).save(any(ParticipationRequest.class));
    }

    @Test
    void addRequest_shouldThrowConflict_whenInitiatorRequestsOwnEvent() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createInitiator()));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 5L)).thenReturn(false);

        assertThrows(ConflictException.class, () -> requestService.addRequest(1L, 5L));

        verify(requestRepository, never()).save(any(ParticipationRequest.class));
    }

    @Test
    void addRequest_shouldThrowConflict_whenEventIsNotPublished() {
        Event event = createEvent(EventState.PENDING, 10, true);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(false);

        assertThrows(ConflictException.class, () -> requestService.addRequest(2L, 5L));

        verify(requestRepository, never()).save(any(ParticipationRequest.class));
    }

    @Test
    void addRequest_shouldThrowConflict_whenParticipantLimitIsReached() {
        Event event = createEvent(EventState.PUBLISHED, 2, true);
        mockMemberAndEvent(event);
        when(requestRepository.existsByRequesterIdAndEventId(2L, 5L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> requestService.addRequest(2L, 5L));

        verify(requestRepository, never()).save(any(ParticipationRequest.class));
    }

    @Test
    void addRequest_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.addRequest(2L, 5L));
    }

    @Test
    void addRequest_shouldThrowNotFound_whenEventDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.addRequest(2L, 5L));
    }

    @Test
    void getUserRequests_shouldReturnRequestsSortedById() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        ParticipationRequest second = createRequest(12L, event, RequestStatus.PENDING);
        ParticipationRequest first = createRequest(11L, event, RequestStatus.CONFIRMED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(requestRepository.findAllByRequesterId(2L)).thenReturn(List.of(second, first));

        List<ParticipationRequestDto> result = requestService.getUserRequests(2L);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getId());
        assertEquals(12L, result.get(1).getId());
    }

    @Test
    void cancelRequest_shouldSetCanceledStatus() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        ParticipationRequest request = createRequest(11L, event, RequestStatus.PENDING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(requestRepository.findByIdAndRequesterId(11L, 2L)).thenReturn(Optional.of(request));

        ParticipationRequestDto result = requestService.cancelRequest(2L, 11L);

        assertEquals("CANCELED", result.getStatus());
    }

    @Test
    void cancelRequest_shouldThrowNotFound_whenRequestBelongsToAnotherUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(requestRepository.findByIdAndRequesterId(11L, 2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.cancelRequest(2L, 11L));
    }

    @Test
    void getEventRequests_shouldReturnRequestsOfEventSortedById() {
        Event event = createEvent(EventState.PUBLISHED, 10, true);
        ParticipationRequest second = createRequest(12L, event, RequestStatus.PENDING);
        ParticipationRequest first = createRequest(11L, event, RequestStatus.PENDING);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventId(5L)).thenReturn(List.of(second, first));

        List<ParticipationRequestDto> result = requestService.getEventRequests(1L, 5L);

        assertEquals(11L, result.get(0).getId());
        assertEquals(12L, result.get(1).getId());
    }

    @Test
    void getEventRequests_shouldThrowNotFound_whenEventBelongsToAnotherUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(eventRepository.findByIdAndInitiatorId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.getEventRequests(2L, 5L));
    }

    @Test
    void updateRequestStatus_shouldConfirmRequest_whenLimitIsNotReached() {
        Event event = createEvent(EventState.PUBLISHED, 2, true);
        ParticipationRequest request = createRequest(11L, event, RequestStatus.PENDING);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(11L))).thenReturn(List.of(request));
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventRequestStatusUpdateDto result = requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED));

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals("CONFIRMED", result.getConfirmedRequests().get(0).getStatus());
        assertTrue(result.getRejectedRequests().isEmpty());
    }

    @Test
    void updateRequestStatus_shouldRejectOtherPendingRequests_whenLimitBecomesReached() {
        Event event = createEvent(EventState.PUBLISHED, 1, true);
        ParticipationRequest confirmed = createRequest(11L, event, RequestStatus.PENDING);
        ParticipationRequest other = createRequest(12L, event, RequestStatus.PENDING);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(11L))).thenReturn(List.of(confirmed));
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.findAllByEventIdAndStatus(5L, RequestStatus.PENDING)).thenReturn(List.of(other));

        EventRequestStatusUpdateDto result = requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED));

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(11L, result.getConfirmedRequests().get(0).getId());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(12L, result.getRejectedRequests().get(0).getId());
        assertEquals("REJECTED", result.getRejectedRequests().get(0).getStatus());
    }

    @Test
    void updateRequestStatus_shouldRejectRequest_whenStatusIsRejected() {
        Event event = createEvent(EventState.PUBLISHED, 5, true);
        ParticipationRequest request = createRequest(11L, event, RequestStatus.PENDING);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(11L))).thenReturn(List.of(request));
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventRequestStatusUpdateDto result = requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.REJECTED));

        assertTrue(result.getConfirmedRequests().isEmpty());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals("REJECTED", result.getRejectedRequests().get(0).getStatus());
    }

    @Test
    void updateRequestStatus_shouldThrowConflict_whenLimitIsAlreadyReached() {
        Event event = createEvent(EventState.PUBLISHED, 1, true);
        ParticipationRequest request = createRequest(11L, event, RequestStatus.PENDING);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(11L))).thenReturn(List.of(request));
        when(requestRepository.countByEventIdAndStatus(5L, RequestStatus.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)));
    }

    @Test
    void updateRequestStatus_shouldThrowConflict_whenRequestIsNotPending() {
        Event event = createEvent(EventState.PUBLISHED, 5, true);
        ParticipationRequest request = createRequest(11L, event, RequestStatus.REJECTED);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(11L))).thenReturn(List.of(request));

        assertThrows(ConflictException.class, () -> requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)));
    }

    @Test
    void updateRequestStatus_shouldThrowNotFound_whenRequestDoesNotBelongToEvent() {
        Event event = createEvent(EventState.PUBLISHED, 5, true);
        mockOwnerAndEvent(event);
        when(requestRepository.findAllByEventIdAndIdIn(5L, Set.of(99L))).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(99L), RequestUpdateStatus.CONFIRMED)));
    }

    @Test
    void updateRequestStatus_shouldReturnEmptyLists_whenParticipantLimitIsZero() {
        Event event = createEvent(EventState.PUBLISHED, 0, true);
        mockOwnerAndEvent(event);

        EventRequestStatusUpdateDto result = requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED));

        assertTrue(result.getConfirmedRequests().isEmpty());
        assertTrue(result.getRejectedRequests().isEmpty());
        verify(requestRepository, never()).findAllByEventIdAndIdIn(anyLong(), any());
    }

    @Test
    void updateRequestStatus_shouldReturnEmptyLists_whenModerationIsDisabled() {
        Event event = createEvent(EventState.PUBLISHED, 5, false);
        mockOwnerAndEvent(event);

        EventRequestStatusUpdateDto result = requestService.updateRequestStatus(1L, 5L,
                createStatusRequest(List.of(11L), RequestUpdateStatus.CONFIRMED));

        assertTrue(result.getConfirmedRequests().isEmpty());
        assertTrue(result.getRejectedRequests().isEmpty());
    }

    private User createInitiator() {
        return User.builder().id(1L).name("Иван Петров").email("ivan@practicummail.ru").build();
    }

    private User createMember() {
        return User.builder().id(2L).name("Мария Сидорова").email("maria@practicummail.ru").build();
    }

    private Event createEvent(EventState state, int participantLimit, boolean requestModeration) {
        return Event.builder()
                .id(5L)
                .title("Концерт группы")
                .initiator(createInitiator())
                .state(state)
                .participantLimit(participantLimit)
                .requestModeration(requestModeration)
                .build();
    }

    private ParticipationRequest createRequest(Long id, Event event, RequestStatus status) {
        return ParticipationRequest.builder()
                .id(id)
                .created(LocalDateTime.now())
                .event(event)
                .requester(createMember())
                .status(status)
                .build();
    }

    private EventRequestStatusUpdateRequest createStatusRequest(List<Long> requestIds,
                                                                RequestUpdateStatus status) {
        return new EventRequestStatusUpdateRequest(requestIds, status);
    }

    private void mockMemberAndEvent(Event event) {
        when(userRepository.findById(2L)).thenReturn(Optional.of(createMember()));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
    }

    private void mockOwnerAndEvent(Event event) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createInitiator()));
        when(eventRepository.findByIdAndInitiatorId(5L, 1L)).thenReturn(Optional.of(event));
    }

    private void mockSave() {
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(100L);
            return request;
        });
    }
}