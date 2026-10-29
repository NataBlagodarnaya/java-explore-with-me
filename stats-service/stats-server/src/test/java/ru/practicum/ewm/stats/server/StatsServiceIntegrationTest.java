package ru.practicum.ewm.stats.server;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.stats.server.model.EndpointHit;
import ru.practicum.ewm.stats.server.service.StatsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatsServiceIntegrationTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private EntityManager em;

    private EndpointHitDto hitDto1;
    private EndpointHitDto hitDto2;
    private EndpointHitDto hitDto3;

    @BeforeEach
    void setUp() {
        hitDto1 = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.10")
                .timestamp("2026-10-28 10:00:00")
                .build();

        hitDto2 = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.10")
                .timestamp("2026-10-28 10:05:00")
                .build();

        hitDto3 = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/2")
                .ip("10.0.0.5")
                .timestamp("2026-10-28 10:10:00")
                .build();
    }

    @Test
    void saveHit_shouldSaveAndPersistRawDataToDatabase() {
        statsService.saveHit(hitDto1);

        List<EndpointHit> result = em.createQuery("SELECT h FROM EndpointHit h", EndpointHit.class).getResultList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApp()).isEqualTo(hitDto1.getApp());
        assertThat(result.get(0).getUri()).isEqualTo(hitDto1.getUri());
        assertThat(result.get(0).getIp()).isEqualTo(hitDto1.getIp());
    }

    @Test
    void getStats_shouldReturnCorrectAggregatedData_whenUniqueFalse() {
        statsService.saveHit(hitDto1);
        statsService.saveHit(hitDto2);
        statsService.saveHit(hitDto3);

        List<ViewStatsDto> stats = statsService.getStats(
                "2026-10-01 00:00:00",
                "2026-11-01 00:00:00",
                null,
                false
        );

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).getUri()).isEqualTo("/events/1");
        assertThat(stats.get(0).getHits()).isEqualTo(2L);
        assertThat(stats.get(1).getUri()).isEqualTo("/events/2");
        assertThat(stats.get(1).getHits()).isEqualTo(1L);
    }

    @Test
    void getStats_shouldReturnUniqueHitsOnly_whenUniqueTrue() {
        statsService.saveHit(hitDto1);
        statsService.saveHit(hitDto2);
        statsService.saveHit(hitDto3);

        List<ViewStatsDto> stats = statsService.getStats(
                "2026-10-01 00:00:00",
                "2026-11-01 00:00:00",
                List.of("/events/1"),
                true
        );

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getUri()).isEqualTo("/events/1");
        assertThat(stats.get(0).getHits()).isEqualTo(1L);
    }
}