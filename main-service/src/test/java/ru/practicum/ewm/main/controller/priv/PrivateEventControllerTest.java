package ru.practicum.ewm.main.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.event.LocationDto;
import ru.practicum.ewm.main.dto.eventrequest.NewEventRequest;
import ru.practicum.ewm.main.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.UserStateAction;
import ru.practicum.ewm.main.service.EventService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateEventController.class)
class PrivateEventControllerTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2030, 1, 15, 12, 0, 0);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EventService eventService;

    @Test
    void addEvent_shouldReturnStatus201AndEvent_whenRequestIsValid() throws Exception {
        when(eventService.addEvent(eq(1L), any(NewEventRequest.class))).thenReturn(createFullEvent(EventState.PENDING));

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(createNewEventRequest()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.title", is("Концерт группы")))
                .andExpect(jsonPath("$.state", is("PENDING")))
                .andExpect(jsonPath("$.category.id", is(2)))
                .andExpect(jsonPath("$.initiator.name", is("Иван Петров")))
                .andExpect(jsonPath("$.eventDate", is("2030-01-15 12:00:00")))
                .andExpect(jsonPath("$.location.lat", notNullValue()))
                .andExpect(jsonPath("$.confirmedRequests", is(0)))
                .andExpect(jsonPath("$.views", is(0)));
    }

    @Test
    void addEvent_shouldSetDefaultValues_whenOptionalFieldsAreNotPassed() throws Exception {
        String body = "{\"annotation\": \"Концерт под открытым небом\", \"category\": 2, "
                + "\"description\": \"Большой концерт для всех желающих\", "
                + "\"eventDate\": \"2030-01-15 12:00:00\", "
                + "\"location\": {\"lat\": 55.75, \"lon\": 37.62}, "
                + "\"title\": \"Концерт группы\"}";

        mvc.perform(post("/users/1/events")
                        .content(body)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        ArgumentCaptor<NewEventRequest> captor = ArgumentCaptor.forClass(NewEventRequest.class);
        verify(eventService).addEvent(eq(1L), captor.capture());
        assertFalse(captor.getValue().getPaid());
        assertEquals(0, captor.getValue().getParticipantLimit());
        assertTrue(captor.getValue().getRequestModeration());
    }

    @Test
    void addEvent_shouldReturnBadRequest_whenAnnotationIsTooShort() throws Exception {
        NewEventRequest request = createNewEventRequest();
        request.setAnnotation("Коротко");

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Field: annotation")));

        verify(eventService, never()).addEvent(any(), any(NewEventRequest.class));
    }

    @Test
    void addEvent_shouldReturnBadRequest_whenTitleIsTooShort() throws Exception {
        NewEventRequest request = createNewEventRequest();
        request.setTitle("аб");

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: title")));
    }

    @Test
    void addEvent_shouldReturnBadRequest_whenCategoryIsMissing() throws Exception {
        NewEventRequest request = createNewEventRequest();
        request.setCategory(null);

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: category")));
    }

    @Test
    void addEvent_shouldReturnBadRequest_whenLocationIsMissing() throws Exception {
        NewEventRequest request = createNewEventRequest();
        request.setLocation(null);

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: location")));
    }

    @Test
    void addEvent_shouldReturnBadRequest_whenParticipantLimitIsNegative() throws Exception {
        NewEventRequest request = createNewEventRequest();
        request.setParticipantLimit(-1);

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: participantLimit")));
    }

    @Test
    void addEvent_shouldReturnConflict_whenEventDateIsTooEarly() throws Exception {
        when(eventService.addEvent(eq(1L), any(NewEventRequest.class)))
                .thenThrow(new ConflictException("Field: eventDate. Error: должно содержать дату, "
                        + "которая еще не наступила. Value: 2030-01-15T12:00"));

        mvc.perform(post("/users/1/events")
                        .content(mapper.writeValueAsString(createNewEventRequest()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.reason", is("For the requested operation the conditions are not met.")));
    }

    @Test
    void addEvent_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(eventService.addEvent(eq(7L), any(NewEventRequest.class)))
                .thenThrow(new NotFoundException("User with id=7 was not found"));

        mvc.perform(post("/users/7/events")
                        .content(mapper.writeValueAsString(createNewEventRequest()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("User with id=7 was not found")));
    }

    @Test
    void getUserEvents_shouldReturnShortEventsAndStatus200() throws Exception {
        when(eventService.getUserEvents(1L, 0, 10)).thenReturn(List.of(createShortEvent()));

        mvc.perform(get("/users/1/events")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("Концерт группы")))
                .andExpect(jsonPath("$[0].eventDate", is("2030-01-15 12:00:00")))
                .andExpect(jsonPath("$[0].initiator.name", is("Иван Петров")));
    }

    @Test
    void getUserEvents_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/users/1/events")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getUserEvent_shouldReturnEventAndStatus200() throws Exception {
        when(eventService.getUserEvent(1L, 10L)).thenReturn(createFullEvent(EventState.PENDING));

        mvc.perform(get("/users/1/events/10")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.annotation", is("Концерт под открытым небом")))
                .andExpect(jsonPath("$.state", is("PENDING")));
    }

    @Test
    void getUserEvent_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        when(eventService.getUserEvent(1L, 99L)).thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(get("/users/1/events/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));
    }

    @Test
    void updateUserEvent_shouldReturnUpdatedEventAndStatus200() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");
        request.setStateAction(UserStateAction.SEND_TO_REVIEW);
        EventFullDto updated = createFullEvent(EventState.PENDING);
        updated.setTitle("Новое название");
        when(eventService.updateUserEvent(eq(1L), eq(10L), any(UpdateEventUserRequest.class))).thenReturn(updated);

        mvc.perform(patch("/users/1/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Новое название")))
                .andExpect(jsonPath("$.state", is("PENDING")));
    }

    @Test
    void updateUserEvent_shouldReturnBadRequest_whenAnnotationIsTooShort() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setAnnotation("Коротко");

        mvc.perform(patch("/users/1/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: annotation")));

        verify(eventService, never()).updateUserEvent(any(), any(), any(UpdateEventUserRequest.class));
    }

    @Test
    void updateUserEvent_shouldReturnBadRequest_whenStateActionIsUnknown() throws Exception {
        mvc.perform(patch("/users/1/events/10")
                        .content("{\"stateAction\": \"UNKNOWN\"}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void updateUserEvent_shouldReturnConflict_whenEventIsPublished() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");
        when(eventService.updateUserEvent(eq(1L), eq(10L), any(UpdateEventUserRequest.class)))
                .thenThrow(new ConflictException("Only pending or canceled events can be changed"));

        mvc.perform(patch("/users/1/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("Only pending or canceled events can be changed")));
    }

    @Test
    void updateUserEvent_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");
        when(eventService.updateUserEvent(eq(1L), eq(99L), any(UpdateEventUserRequest.class)))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(patch("/users/1/events/99")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")));
    }

    private NewEventRequest createNewEventRequest() {
        NewEventRequest request = new NewEventRequest();
        request.setAnnotation("Концерт под открытым небом");
        request.setCategory(2L);
        request.setDescription("Большой концерт для всех желающих");
        request.setEventDate(EVENT_DATE);
        request.setLocation(new LocationDto(55.75f, 37.62f));
        request.setPaid(false);
        request.setParticipantLimit(10);
        request.setRequestModeration(true);
        request.setTitle("Концерт группы");
        return request;
    }

    private EventFullDto createFullEvent(EventState state) {
        return EventFullDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.of(2029, 12, 1, 10, 0, 0))
                .description("Большой концерт для всех желающих")
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .location(new LocationDto(55.75f, 37.62f))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .title("Концерт группы")
                .views(0L)
                .build();
    }

    private EventShortDto createShortEvent() {
        return EventShortDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(0L)
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .paid(false)
                .title("Концерт группы")
                .views(0L)
                .build();
    }
}