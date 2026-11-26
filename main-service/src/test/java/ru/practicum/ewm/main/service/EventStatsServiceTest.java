package ru.practicum.ewm.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventStatsServiceTest {

    private StatsClient statsClient;
    private EventStatsService eventStatsService;

    @BeforeEach
    void setUp() {
        statsClient = Mockito.mock(StatsClient.class);
        eventStatsService = new EventStatsService(statsClient, new ObjectMapper());
    }

    @Test
    void saveHit_shouldSendHitWithAppUriIpAndTimestamp() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events/5");
        request.setRemoteAddr("192.168.1.10");

        eventStatsService.saveHit(request);

        ArgumentCaptor<EndpointHitDto> captor = ArgumentCaptor.forClass(EndpointHitDto.class);
        verify(statsClient).saveHit(captor.capture());
        assertEquals("ewm-main-service", captor.getValue().getApp());
        assertEquals("/events/5", captor.getValue().getUri());
        assertEquals("192.168.1.10", captor.getValue().getIp());
        assertTrue(captor.getValue().getTimestamp().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void saveHit_shouldNotThrow_whenStatsServiceIsUnavailable() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        when(statsClient.saveHit(any(EndpointHitDto.class))).thenThrow(new RuntimeException("Статистика недоступна"));

        assertDoesNotThrow(() -> eventStatsService.saveHit(request));
    }

    @Test
    void getViews_shouldReturnEmptyMapWithoutRequest_whenThereAreNoEvents() {
        Map<Long, Long> result = eventStatsService.getViews(List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(statsClient);
    }

    @Test
    void getViews_shouldConvertStatsToMapByEventId() {
        Event first = createEvent(1L, LocalDateTime.of(2029, 12, 2, 9, 0, 0));
        Event second = createEvent(2L, LocalDateTime.of(2029, 12, 3, 9, 0, 0));
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(ResponseEntity.ok(List.of(
                Map.of("app", "ewm-main-service", "uri", "/events/1", "hits", 5),
                Map.of("app", "ewm-main-service", "uri", "/events/2", "hits", 3))));

        Map<Long, Long> result = eventStatsService.getViews(List.of(first, second));

        assertEquals(2, result.size());
        assertEquals(5L, result.get(1L));
        assertEquals(3L, result.get(2L));
    }

    @Test
    void getViews_shouldAskStatsFromEarliestPublicationForAllEventUris() {
        Event published = createEvent(1L, LocalDateTime.of(2029, 12, 2, 9, 0, 0));
        Event notPublished = createEvent(2L, null);
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(ResponseEntity.ok(List.of()));

        eventStatsService.getViews(List.of(published, notPublished));

        ArgumentCaptor<String> startCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> urisCaptor = ArgumentCaptor.forClass(List.class);
        verify(statsClient).getStats(startCaptor.capture(), anyString(), urisCaptor.capture(), eq(true));
        assertEquals("2029-12-01 10:00:00", startCaptor.getValue());
        assertEquals(List.of("/events/1", "/events/2"), urisCaptor.getValue());
    }

    @Test
    void getViews_shouldReturnEmptyMap_whenStatsServiceRespondsWithError() {
        Event event = createEvent(1L, LocalDateTime.of(2029, 12, 2, 9, 0, 0));
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(ResponseEntity.<Object>status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        Map<Long, Long> result = eventStatsService.getViews(List.of(event));

        assertTrue(result.isEmpty());
    }

    @Test
    void getViews_shouldReturnEmptyMap_whenResponseHasNoBody() {
        Event event = createEvent(1L, LocalDateTime.of(2029, 12, 2, 9, 0, 0));
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(ResponseEntity.<Object>ok().build());

        Map<Long, Long> result = eventStatsService.getViews(List.of(event));

        assertTrue(result.isEmpty());
    }

    @Test
    void getViews_shouldReturnEmptyMap_whenStatsServiceIsUnavailable() {
        Event event = createEvent(1L, LocalDateTime.of(2029, 12, 2, 9, 0, 0));
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenThrow(new RuntimeException("Статистика недоступна"));

        Map<Long, Long> result = eventStatsService.getViews(List.of(event));

        assertTrue(result.isEmpty());
    }

    private Event createEvent(Long id, LocalDateTime publishedOn) {
        return Event.builder()
                .id(id)
                .title("Концерт группы")
                .createdOn(LocalDateTime.of(2029, 12, 1, 10, 0, 0))
                .publishedOn(publishedOn)
                .build();
    }
}