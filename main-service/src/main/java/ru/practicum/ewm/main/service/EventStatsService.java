package ru.practicum.ewm.main.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.util.DateTimeConstants;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatsService {

    private static final String APP_NAME = "ewm-main-service";
    private static final String EVENTS_URI = "/events/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateTimeConstants.DATE_TIME_PATTERN);

    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    public void saveHit(HttpServletRequest request) {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
        try {
            statsClient.saveHit(hit);
        } catch (RuntimeException e) {
            log.warn("Не удалось сохранить статистику для {}: {}", hit.getUri(), e.getMessage());
        }
    }

    public Map<Long, Long> getViews(Collection<Event> events) {
        Map<Long, Long> views = new HashMap<>();
        if (events.isEmpty()) {
            return views;
        }

        LocalDateTime start = events.stream()
                .map(event -> event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn())
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());
        List<String> uris = events.stream()
                .map(event -> EVENTS_URI + event.getId())
                .toList();

        try {
            ResponseEntity<Object> response = statsClient.getStats(start.format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER), uris, true);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(),
                        new TypeReference<>() { });
                for (ViewStatsDto stat : stats) {
                    views.put(Long.parseLong(stat.getUri().substring(EVENTS_URI.length())), stat.getHits());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
        }
        return views;
    }
}