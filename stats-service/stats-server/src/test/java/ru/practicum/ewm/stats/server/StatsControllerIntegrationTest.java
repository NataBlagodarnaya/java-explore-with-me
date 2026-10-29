package ru.practicum.ewm.stats.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.stats.dto.EndpointHitDto;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StatsControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private EndpointHitDto hitDto;

    @BeforeEach
    void setUp() {
        hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.10")
                .timestamp("2026-10-28 10:00:00")
                .build();
    }

    @Test
    void hitEndpoint_shouldReturnStatus201Created_whenValidBodySent() {
        ResponseEntity<Void> response = restTemplate.postForEntity("/hit", hitDto, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void statsEndpoint_shouldReturnBadRequestStatus_whenStartAfterEndSent() {
        String url = "/stats?start={start}&end={end}&unique={unique}";
        Map<String, String> params = Map.of(
                "start", "2026-11-01 00:00:00",
                "end", "2026-10-01 00:00:00",
                "unique", "false"
        );

        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class, params);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}