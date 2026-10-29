package ru.practicum.ewm.stats.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class EndpointHitJsonTest {

    @Autowired
    private JacksonTester<EndpointHitDto> hitJson;

    @Autowired
    private JacksonTester<ViewStatsDto> statsJson;

    @Test
    void testEndpointHitDtoSerialization() throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/99")
                .ip("172.16.0.1")
                .timestamp("2026-10-28 15:30:00")
                .build();

        JsonContent<EndpointHitDto> result = hitJson.write(dto);

        assertThat(result).hasJsonPathStringValue("$.app", "ewm-main-service");
        assertThat(result).hasJsonPathStringValue("$.uri", "/events/99");
        assertThat(result).hasJsonPathStringValue("$.ip", "172.16.0.1");
        assertThat(result).hasJsonPathStringValue("$.timestamp", "2026-10-28 15:30:00");
    }

    @Test
    void testEndpointHitDtoDeserialization() throws Exception {
        String jsonContent = "{\n" +
                "  \"app\": \"ewm-main-service\",\n" +
                "  \"uri\": \"/events/99\",\n" +
                "  \"ip\": \"172.16.0.1\",\n" +
                "  \"timestamp\": \"2026-10-28 15:30:00\"\n" +
                "}";

        EndpointHitDto result = hitJson.parse(jsonContent).getObject();

        assertThat(result.getApp()).isEqualTo("ewm-main-service");
        assertThat(result.getUri()).isEqualTo("/events/99");
        assertThat(result.getIp()).isEqualTo("172.16.0.1");
        assertThat(result.getTimestamp()).isEqualTo("2026-10-28 15:30:00");
    }

    @Test
    void testViewStatsDtoSerialization() throws Exception {
        ViewStatsDto dto = ViewStatsDto.builder()
                .app("ewm-main-service")
                .uri("/events/99")
                .hits(15L)
                .build();

        JsonContent<ViewStatsDto> result = statsJson.write(dto);

        assertThat(result).hasJsonPathStringValue("$.app", "ewm-main-service");
        assertThat(result).hasJsonPathStringValue("$.uri", "/events/99");
        assertThat(result).hasJsonPathNumberValue("$.hits", 15);
    }
}