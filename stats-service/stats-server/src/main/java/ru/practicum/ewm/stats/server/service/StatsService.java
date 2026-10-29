package ru.practicum.ewm.stats.server.service;

import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

import java.util.List;

public interface StatsService {
    void saveHit(EndpointHitDto hitDto);
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}