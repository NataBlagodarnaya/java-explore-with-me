package ru.practicum.ewm.main.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.model.EventSort;
import ru.practicum.ewm.main.service.EventService;
import ru.practicum.ewm.main.service.EventStatsService;
import ru.practicum.ewm.main.util.DateTimeConstants;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {

    private final EventService eventService;
    private final EventStatsService eventStatsService;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) @Size(min = 1, max = 7000) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN) LocalDateTime rangeStart,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN) LocalDateTime rangeEnd,
                                         @RequestParam(defaultValue = "false") boolean onlyAvailable,
                                         @RequestParam(required = false) EventSort sort,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                         @RequestParam(defaultValue = "10") @Positive int size,
                                         HttpServletRequest request) {
        log.info("Публичный поиск событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, "
                        + "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        List<EventShortDto> events = eventService.getPublicEvents(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, from, size);
        eventStatsService.saveHit(request);
        return events;
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("Получение опубликованного события {}", id);
        EventFullDto event = eventService.getPublicEvent(id);
        eventStatsService.saveHit(request);
        return event;
    }
}