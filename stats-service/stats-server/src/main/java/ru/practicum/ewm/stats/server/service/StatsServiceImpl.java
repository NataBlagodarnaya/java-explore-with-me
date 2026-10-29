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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение хита для URI: {}", hitDto.getUri());
        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(LocalDateTime.parse(hitDto.getTimestamp(), formatter))
                .build();
        statsRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            throw new ValidationException(
                    "Параметры start и end обязательны для заполнения и не могут быть пустыми"
            );
        }

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {
            startTime = LocalDateTime.parse(start, formatter);
            endTime = LocalDateTime.parse(end, formatter);
        } catch (Exception e) {
            throw new ValidationException(
                    "Неверный формат даты. Используйте формат: yyyy-MM-dd HH:mm:ss"
            );
        }

        if (startTime.isAfter(endTime)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
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
                log.info("Получение статистики для указанных URI (только unique IP): {}", uris);
                return statsRepository.getStatsByUrisUnique(startTime, endTime, uris);
            } else {
                log.info("Получение статистики для указанных URI (все посещения): {}", uris);
                return statsRepository.getStatsByUris(startTime, endTime, uris);
            }
        }
    }
}