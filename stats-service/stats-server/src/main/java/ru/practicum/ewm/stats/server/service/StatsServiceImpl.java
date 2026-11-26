package ru.practicum.ewm.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.stats.server.exception.ValidationException;
import ru.practicum.ewm.stats.server.model.EndpointHit;
import ru.practicum.ewm.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение хита для URI: {}", hitDto.getUri());
        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(parseDate(hitDto.getTimestamp()))
                .build();
        statsRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            throw new ValidationException("Parameters start and end are required and must not be blank");
        }

        LocalDateTime startTime = parseDate(start);
        LocalDateTime endTime = parseDate(end);

        if (startTime.isAfter(endTime)) {
            throw new ValidationException("Start date must not be after end date");
        }

        if (uris == null || uris.isEmpty()) {
            if (unique) {
                log.info("Получение статистики для всех URI (только уникальные IP)");
                return statsRepository.getStatsAllUnique(startTime, endTime);
            } else {
                log.info("Получение статистики для всех URI (все посещения)");
                return statsRepository.getStatsAll(startTime, endTime);
            }
        } else {
            if (unique) {
                log.info("Получение статистики для указанных URI (только уникальные IP): {}", uris);
                return statsRepository.getStatsByUrisUnique(startTime, endTime, uris);
            } else {
                log.info("Получение статистики для указанных URI (все посещения): {}", uris);
                return statsRepository.getStatsByUris(startTime, endTime, uris);
            }
        }
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format, expected yyyy-MM-dd HH:mm:ss. Value: " + value);
        }
    }
}