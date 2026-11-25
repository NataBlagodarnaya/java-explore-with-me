package ru.practicum.ewm.main.controller.priv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.eventrequest.ParticipationRequestDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.RequestService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateRequestController.class)
class PrivateRequestControllerTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2030, 1, 15, 12, 0, 0);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RequestService requestService;

    @Test
    void getUserRequests_shouldReturnRequestsAndStatus200() throws Exception {
        when(requestService.getUserRequests(2L)).thenReturn(List.of(
                createRequest(11L, "PENDING"),
                createRequest(12L, "CONFIRMED")));

        mvc.perform(get("/users/2/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(11)))
                .andExpect(jsonPath("$[0].event", is(5)))
                .andExpect(jsonPath("$[0].requester", is(2)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[0].created", is("2030-01-15T12:00:00")))
                .andExpect(jsonPath("$[1].status", is("CONFIRMED")));
    }

    @Test
    void getUserRequests_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(requestService.getUserRequests(7L)).thenThrow(new NotFoundException("User with id=7 was not found"));

        mvc.perform(get("/users/7/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("User with id=7 was not found")));
    }

    @Test
    void getUserRequests_shouldReturnBadRequest_whenUserIdIsNotNumber() throws Exception {
        mvc.perform(get("/users/abc/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void addRequest_shouldReturnStatus201AndRequest() throws Exception {
        when(requestService.addRequest(2L, 5L)).thenReturn(createRequest(11L, "PENDING"));

        mvc.perform(post("/users/2/requests")
                        .param("eventId", "5")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(11)))
                .andExpect(jsonPath("$.event", is(5)))
                .andExpect(jsonPath("$.requester", is(2)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void addRequest_shouldReturnBadRequest_whenEventIdIsMissing() throws Exception {
        mvc.perform(post("/users/2/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void addRequest_shouldReturnBadRequest_whenEventIdIsNotNumber() throws Exception {
        mvc.perform(post("/users/2/requests")
                        .param("eventId", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void addRequest_shouldReturnConflict_whenParticipantLimitIsReached() throws Exception {
        when(requestService.addRequest(2L, 5L))
                .thenThrow(new ConflictException("The participant limit has been reached"));

        mvc.perform(post("/users/2/requests")
                        .param("eventId", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.reason", is("For the requested operation the conditions are not met.")))
                .andExpect(jsonPath("$.message", is("The participant limit has been reached")));
    }

    @Test
    void addRequest_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        when(requestService.addRequest(2L, 99L)).thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(post("/users/2/requests")
                        .param("eventId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));
    }

    @Test
    void cancelRequest_shouldReturnCanceledRequestAndStatus200() throws Exception {
        when(requestService.cancelRequest(2L, 11L)).thenReturn(createRequest(11L, "CANCELED"));

        mvc.perform(patch("/users/2/requests/11/cancel")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(11)))
                .andExpect(jsonPath("$.status", is("CANCELED")));
    }

    @Test
    void cancelRequest_shouldReturnNotFound_whenRequestDoesNotExist() throws Exception {
        when(requestService.cancelRequest(2L, 99L))
                .thenThrow(new NotFoundException("Request with id=99 was not found"));

        mvc.perform(patch("/users/2/requests/99/cancel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Request with id=99 was not found")));
    }

    private ParticipationRequestDto createRequest(Long id, String status) {
        return ParticipationRequestDto.builder()
                .id(id)
                .created(CREATED)
                .event(5L)
                .requester(2L)
                .status(status)
                .build();
    }
}