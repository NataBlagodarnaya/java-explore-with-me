package ru.practicum.ewm.stats.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.stats.dto.EndpointHitDto;

import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class StatsClientTest {

    private StatsClient statsClient;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper;

    private EndpointHitDto hitDto;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate(
                new SimpleClientHttpRequestFactory()
        );
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory("http://localhost:9090")
        );

        server = MockRestServiceServer.bindTo(restTemplate).build();

        statsClient = new StatsClient(restTemplate);

        objectMapper = new ObjectMapper();

        hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.10")
                .timestamp("2026-10-28 10:00:00")
                .build();
    }

    @Test
    void saveHit_shouldSendPostRequest() throws Exception {
        server.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        objectMapper.writeValueAsString(hitDto)
                ))
                .andRespond(withStatus(HttpStatus.CREATED));

        statsClient.saveHit(hitDto);

        server.verify();
    }

    @Test
    void getStats_shouldSendGetRequest() {
        server.expect(requestTo(
                        "http://localhost:9090/stats?start=2020-01-01%2000%3A00%3A00"
                                + "&end=2035-01-01%2000%3A00%3A00"
                                + "&uris=%2Fevents%2F1"
                                + "&unique=false"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        statsClient.getStats(
                "2020-01-01 00:00:00",
                "2035-01-01 00:00:00",
                List.of("/events/1"),
                false
        );

        server.verify();
    }
}