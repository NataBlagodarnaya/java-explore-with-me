package ru.practicum.ewm.main.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateDto;
import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.eventrequest.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.RequestUpdateStatus;
import ru.practicum.ewm.main.service.RequestService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateEventRequestController.class)
class PrivateEventRequestControllerTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2030, 1, 15, 12, 0, 0);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RequestService requestService;

    @Test
    void getEventRequests_shouldReturnRequestsAndStatus200() throws Exception {
        when(requestService.getEventRequests(1L, 5L)).thenReturn(List.of(
                createRequest(11L, 2L, "PENDING"),
                createRequest(12L, 3L, "CONFIRMED")));

        mvc.perform(get("/users/1/events/5/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(11)))
                .andExpect(jsonPath("$[0].requester", is(2)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[1].status", is("CONFIRMED")));
    }

    @Test
    void getEventRequests_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        when(requestService.getEventRequests(1L, 99L))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(get("/users/1/events/99/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));
    }

    @Test
    void updateRequestStatus_shouldReturnConfirmedAndRejectedLists() throws Exception {
        EventRequestStatusUpdateDto response = new EventRequestStatusUpdateDto(
                List.of(createRequest(11L, 2L, "CONFIRMED")),
                List.of(createRequest(12L, 3L, "REJECTED")));
        when(requestService.updateRequestStatus(eq(1L), eq(5L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(response);

        mvc.perform(patch("/users/1/events/5/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(1)))
                .andExpect(jsonPath("$.confirmedRequests[0].id", is(11)))
                .andExpect(jsonPath("$.confirmedRequests[0].status", is("CONFIRMED")))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(1)))
                .andExpect(jsonPath("$.rejectedRequests[0].id", is(12)))
                .andExpect(jsonPath("$.rejectedRequests[0].status", is("REJECTED")));
    }

    @Test
    void updateRequestStatus_shouldReturnEmptyLists_whenNothingToConfirm() throws Exception {
        when(requestService.updateRequestStatus(eq(1L), eq(5L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(new EventRequestStatusUpdateDto(List.of(), List.of()));

        mvc.perform(patch("/users/1/events/5/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(0)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));
    }

    @Test
    void updateRequestStatus_shouldPassIdsAndStatusToService() throws Exception {
        when(requestService.updateRequestStatus(eq(1L), eq(5L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(new EventRequestStatusUpdateDto(List.of(), List.of()));

        mvc.perform(patch("/users/1/events/5/requests")
                        .content("{\"requestIds\": [11, 12], \"status\": \"REJECTED\"}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<EventRequestStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(EventRequestStatusUpdateRequest.class);
        verify(requestService).updateRequestStatus(eq(1L), eq(5L), captor.capture());
        assertEquals(List.of(11L, 12L), captor.getValue().getRequestIds());
        assertEquals(RequestUpdateStatus.REJECTED, captor.getValue().getStatus());
    }

    @Test
    void updateRequestStatus_shouldReturnBadRequest_whenRequestIdsAreEmpty() throws Exception {
        mvc.perform(patch("/users/1/events/5/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Field: requestIds")));

        verify(requestService, never()).updateRequestStatus(any(), any(), any());
    }

    @Test
    void updateRequestStatus_shouldReturnBadRequest_whenStatusIsMissing() throws Exception {
        mvc.perform(patch("/users/1/events/5/requests")
                        .content("{\"requestIds\": [11]}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: status")));
    }

    @Test
    void updateRequestStatus_shouldReturnBadRequest_whenStatusIsNotAllowed() throws Exception {
        mvc.perform(patch("/users/1/events/5/requests")
                        .content("{\"requestIds\": [11], \"status\": \"PENDING\"}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));

        verify(requestService, never()).updateRequestStatus(any(), any(), any());
    }

    @Test
    void updateRequestStatus_shouldReturnConflict_whenParticipantLimitIsReached() throws Exception {
        when(requestService.updateRequestStatus(eq(1L), eq(5L), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException("The participant limit has been reached"));

        mvc.perform(patch("/users/1/events/5/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("The participant limit has been reached")));
    }

    @Test
    void updateRequestStatus_shouldReturnConflict_whenRequestIsNotPending() throws Exception {
        when(requestService.updateRequestStatus(eq(1L), eq(5L), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException("Request must have status PENDING"));

        mvc.perform(patch("/users/1/events/5/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Request must have status PENDING")));
    }

    @Test
    void updateRequestStatus_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        when(requestService.updateRequestStatus(eq(1L), eq(99L), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(patch("/users/1/events/99/requests")
                        .content(mapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(11L), RequestUpdateStatus.CONFIRMED)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")));
    }

    private ParticipationRequestDto createRequest(Long id, Long requesterId, String status) {
        return ParticipationRequestDto.builder()
                .id(id)
                .created(CREATED)
                .event(5L)
                .requester(requesterId)
                .status(status)
                .build();
    }
}