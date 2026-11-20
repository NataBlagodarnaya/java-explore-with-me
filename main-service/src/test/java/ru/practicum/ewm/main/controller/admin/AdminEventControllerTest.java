package ru.practicum.ewm.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.AdminStateAction;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.service.EventService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminEventController.class)
class AdminEventControllerTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2030, 1, 15, 12, 0, 0);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EventService eventService;

    @Test
    void getEvents_shouldReturnEventsAndStatus200_whenAllFiltersArePassed() throws Exception {
        when(eventService.getEventsByAdmin(List.of(1L, 2L), List.of(EventState.PENDING), List.of(3L),
                LocalDateTime.of(2030, 1, 1, 0, 0, 0), LocalDateTime.of(2030, 2, 1, 0, 0, 0), 0, 10))
                .thenReturn(List.of(createFullEvent(EventState.PENDING)));

        mvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", "PENDING")
                        .param("categories", "3")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2030-02-01 00:00:00")
                        .param("from", "0")
                        .param("size", "10")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].state", is("PENDING")))
                .andExpect(jsonPath("$[0].eventDate", is("2030-01-15 12:00:00")))
                .andExpect(jsonPath("$[0].initiator.name", is("Иван Петров")));
    }

    @Test
    void getEvents_shouldUseDefaultValues_whenParamsAreNotPassed() throws Exception {
        when(eventService.getEventsByAdmin(null, null, null, null, null, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/admin/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(eventService).getEventsByAdmin(null, null, null, null, null, 0, 10);
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenStateIsUnknown() throws Exception {
        mvc.perform(get("/admin/events")
                        .param("states", "UNKNOWN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenDateFormatIsWrong() throws Exception {
        mvc.perform(get("/admin/events")
                        .param("rangeStart", "01.01.2030")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/admin/events")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenRangeEndIsBeforeRangeStart() throws Exception {
        when(eventService.getEventsByAdmin(null, null, null, LocalDateTime.of(2030, 2, 1, 0, 0, 0),
                LocalDateTime.of(2030, 1, 1, 0, 0, 0), 0, 10))
                .thenThrow(new BadRequestException("Range end must not be before range start"));

        mvc.perform(get("/admin/events")
                        .param("rangeStart", "2030-02-01 00:00:00")
                        .param("rangeEnd", "2030-01-01 00:00:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Range end must not be before range start")));
    }

    @Test
    void updateEvent_shouldReturnPublishedEventAndStatus200() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);
        when(eventService.updateEventByAdmin(eq(10L), any(UpdateEventAdminRequest.class)))
                .thenReturn(createFullEvent(EventState.PUBLISHED));

        mvc.perform(patch("/admin/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.state", is("PUBLISHED")));
    }

    @Test
    void updateEvent_shouldReturnBadRequest_whenTitleIsTooShort() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("аб");

        mvc.perform(patch("/admin/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: title")));
    }

    @Test
    void updateEvent_shouldReturnBadRequest_whenStateActionIsUnknown() throws Exception {
        mvc.perform(patch("/admin/events/10")
                        .content("{\"stateAction\": \"UNKNOWN\"}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void updateEvent_shouldReturnConflict_whenEventIsNotPending() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);
        when(eventService.updateEventByAdmin(eq(10L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ConflictException("Cannot publish the event because it's not in the right state: PUBLISHED"));

        mvc.perform(patch("/admin/events/10")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.message",
                        is("Cannot publish the event because it's not in the right state: PUBLISHED")));
    }

    @Test
    void updateEvent_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.REJECT_EVENT);
        when(eventService.updateEventByAdmin(eq(99L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(patch("/admin/events/99")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));
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
}