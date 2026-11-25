package ru.practicum.ewm.main.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.event.LocationDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.BadRequestException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.EventSort;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.service.EventService;
import ru.practicum.ewm.main.service.EventStatsService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicEventController.class)
class PublicEventControllerTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2030, 1, 15, 12, 0, 0);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventStatsService eventStatsService;

    @Test
    void getEvents_shouldReturnShortEventsAndSaveHit() throws Exception {
        when(eventService.getPublicEvents(null, null, null, null, null, false, null, 0, 10))
                .thenReturn(List.of(createShortEvent()));

        mvc.perform(get("/events")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("Концерт группы")))
                .andExpect(jsonPath("$[0].eventDate", is("2030-01-15 12:00:00")))
                .andExpect(jsonPath("$[0].views", is(5)))
                .andExpect(jsonPath("$[0].confirmedRequests", is(2)));

        verify(eventStatsService).saveHit(any(HttpServletRequest.class));
    }

    @Test
    void getEvents_shouldPassAllParamsToService() throws Exception {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2030, 2, 1, 0, 0, 0);
        when(eventService.getPublicEvents("концерт", List.of(1L, 2L), true, start, end, true,
                EventSort.VIEWS, 1, 5)).thenReturn(List.of(createShortEvent()));

        mvc.perform(get("/events")
                        .param("text", "концерт")
                        .param("categories", "1", "2")
                        .param("paid", "true")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2030-02-01 00:00:00")
                        .param("onlyAvailable", "true")
                        .param("sort", "VIEWS")
                        .param("from", "1")
                        .param("size", "5")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenTextIsEmpty() throws Exception {
        mvc.perform(get("/events")
                        .param("text", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));

        verify(eventStatsService, never()).saveHit(any(HttpServletRequest.class));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenSortIsUnknown() throws Exception {
        mvc.perform(get("/events")
                        .param("sort", "WRONG")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/events")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenFromIsNegative() throws Exception {
        mvc.perform(get("/events")
                        .param("from", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequest_whenDateFormatIsWrong() throws Exception {
        mvc.perform(get("/events")
                        .param("rangeStart", "01.01.2030")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getEvents_shouldReturnBadRequestAndNotSaveHit_whenServiceRejectsRange() throws Exception {
        LocalDateTime start = LocalDateTime.of(2030, 2, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2030, 1, 1, 0, 0, 0);
        when(eventService.getPublicEvents(null, null, null, start, end, false, null, 0, 10))
                .thenThrow(new BadRequestException("Range end must not be before range start"));

        mvc.perform(get("/events")
                        .param("rangeStart", "2030-02-01 00:00:00")
                        .param("rangeEnd", "2030-01-01 00:00:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Range end must not be before range start")));

        verify(eventStatsService, never()).saveHit(any(HttpServletRequest.class));
    }

    @Test
    void getEvent_shouldReturnFullEventAndSaveHit() throws Exception {
        when(eventService.getPublicEvent(10L)).thenReturn(createFullEvent());

        mvc.perform(get("/events/10")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.state", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedOn", is("2029-12-02 09:00:00")))
                .andExpect(jsonPath("$.views", is(5)))
                .andExpect(jsonPath("$.confirmedRequests", is(2)));

        verify(eventStatsService).saveHit(any(HttpServletRequest.class));
    }

    @Test
    void getEvent_shouldReturnNotFoundAndNotSaveHit_whenEventDoesNotExist() throws Exception {
        when(eventService.getPublicEvent(99L)).thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(get("/events/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));

        verify(eventStatsService, never()).saveHit(any(HttpServletRequest.class));
    }

    @Test
    void getEvent_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(get("/events/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    private EventShortDto createShortEvent() {
        return EventShortDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(2L)
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .paid(false)
                .title("Концерт группы")
                .views(5L)
                .build();
    }

    private EventFullDto createFullEvent() {
        return EventFullDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(2L)
                .createdOn(LocalDateTime.of(2029, 12, 1, 10, 0, 0))
                .description("Большой концерт для всех желающих")
                .eventDate(EVENT_DATE)
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .location(new LocationDto(55.75f, 37.62f))
                .paid(false)
                .participantLimit(10)
                .publishedOn(LocalDateTime.of(2029, 12, 2, 9, 0, 0))
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Концерт группы")
                .views(5L)
                .build();
    }
}