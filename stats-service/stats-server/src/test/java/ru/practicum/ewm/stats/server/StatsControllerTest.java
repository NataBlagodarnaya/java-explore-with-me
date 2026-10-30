package ru.practicum.ewm.stats.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.stats.server.controller.StatsController;
import ru.practicum.ewm.stats.server.exception.ValidationException;
import ru.practicum.ewm.stats.server.service.StatsService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private StatsService statsService;

    private EndpointHitDto hitDto;
    private ViewStatsDto statsDto;

    @BeforeEach
    void setUp() {
        hitDto = new EndpointHitDto();
        hitDto.setApp("ewm-main-service");
        hitDto.setUri("/events/1");
        hitDto.setIp("192.168.1.10");
        hitDto.setTimestamp("2026-10-28 10:00:00");

        statsDto = new ViewStatsDto("ewm-main-service", "/events/1", 5L);
    }

    @Test
    void saveHit_shouldReturnStatus201_whenValidDto() throws Exception {
        mvc.perform(post("/hit")
                        .content(mapper.writeValueAsString(hitDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Mockito.verify(statsService, Mockito.times(1)).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void getStats_shouldReturnStatsListAndStatus200_whenValidParams() throws Exception {
        Mockito.when(statsService.getStats(anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(List.of(statsDto));

        mvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2035-01-01 00:00:00")
                        .param("unique", "false")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].app", is(statsDto.getApp())))
                .andExpect(jsonPath("$[0].uri", is(statsDto.getUri())))
                .andExpect(jsonPath("$[0].hits", is(statsDto.getHits().intValue())));
    }


    @Test
    void getStats_shouldReturnBadRequest_whenValidationExceptionThrown() throws Exception {
        Mockito.when(statsService.getStats(anyString(), anyString(), any(), anyBoolean()))
                .thenThrow(new ValidationException("Дата начала не может быть позже даты окончания"));

        mvc.perform(get("/stats")
                        .param("start", "2035-01-01 00:00:00")
                        .param("end", "2020-01-01 00:00:00")
                        .param("unique", "false")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Ошибка валидации: Дата начала не может быть позже даты окончания")));
    }
}