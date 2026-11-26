package ru.practicum.ewm.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.category.CategoryRequest;
import ru.practicum.ewm.main.dto.event.LocationDto;
import ru.practicum.ewm.main.dto.eventrequest.NewEventRequest;
import ru.practicum.ewm.main.dto.user.UserRequest;
import ru.practicum.ewm.stats.client.StatsClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MainServiceIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        when(statsClient.getStats(anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(ResponseEntity.ok(List.of()));
    }

    @Test
    void participationFlow_shouldConfirmRequestAndRefuseOverLimit() throws Exception {
        long ownerId = createUser("Иван Петров", "ivan@practicummail.ru");
        long firstMemberId = createUser("Мария Сидорова", "maria@practicummail.ru");
        long secondMemberId = createUser("Пётр Иванов", "petr@practicummail.ru");
        long categoryId = createCategory("Концерты");
        long eventId = createEvent(ownerId, categoryId, 1, true, "Концерт группы");
        publishEvent(eventId);

        MvcResult requestResult = mvc.perform(post("/users/" + firstMemberId + "/requests")
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.event", is((int) eventId)))
                .andExpect(jsonPath("$.requester", is((int) firstMemberId)))
                .andReturn();
        long requestId = extractId(requestResult);

        mvc.perform(get("/users/" + ownerId + "/events/" + eventId + "/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) requestId)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        mvc.perform(patch("/users/" + ownerId + "/events/" + eventId + "/requests")
                        .content("{\"requestIds\": [" + requestId + "], \"status\": \"CONFIRMED\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(1)))
                .andExpect(jsonPath("$.confirmedRequests[0].status", is("CONFIRMED")))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));

        mvc.perform(get("/users/" + ownerId + "/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", is(1)));

        mvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("PUBLISHED")))
                .andExpect(jsonPath("$.confirmedRequests", is(1)))
                .andExpect(jsonPath("$.views", is(0)));

        mvc.perform(post("/users/" + secondMemberId + "/requests")
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("The participant limit has been reached")));
    }

    @Test
    void request_shouldBeConfirmedAutomatically_whenModerationIsDisabled() throws Exception {
        long ownerId = createUser("Иван Петров", "ivan@practicummail.ru");
        long memberId = createUser("Мария Сидорова", "maria@practicummail.ru");
        long categoryId = createCategory("Концерты");
        long eventId = createEvent(ownerId, categoryId, 5, false, "Открытый концерт");
        publishEvent(eventId);

        mvc.perform(post("/users/" + memberId + "/requests")
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        mvc.perform(get("/users/" + ownerId + "/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", is(1)));
    }

    @Test
    void publicSearch_shouldReturnOnlyPublishedEventsAndSaveHit() throws Exception {
        long ownerId = createUser("Иван Петров", "ivan@practicummail.ru");
        long categoryId = createCategory("Концерты");
        long publishedId = createEvent(ownerId, categoryId, 0, true, "Опубликованный фестиваль");
        createEvent(ownerId, categoryId, 0, true, "Неопубликованный фестиваль");
        publishEvent(publishedId);

        mvc.perform(get("/events")
                        .param("text", "ФЕСТИВАЛЬ")
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) publishedId)))
                .andExpect(jsonPath("$[0].title", is("Опубликованный фестиваль")));

        verify(statsClient).saveHit(any());
    }

    @Test
    void compilation_shouldContainEventAndNotDeleteEventWhenRemoved() throws Exception {
        long ownerId = createUser("Иван Петров", "ivan@practicummail.ru");
        long categoryId = createCategory("Концерты");
        long eventId = createEvent(ownerId, categoryId, 0, true, "Концерт группы");

        MvcResult compilationResult = mvc.perform(post("/admin/compilations")
                        .content("{\"title\": \"Летние концерты\", \"pinned\": true, \"events\": [" + eventId + "]}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andReturn();
        long compilationId = extractId(compilationResult);

        mvc.perform(get("/compilations/" + compilationId)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Летние концерты")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events[0].id", is((int) eventId)));

        mvc.perform(delete("/admin/compilations/" + compilationId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/compilations/" + compilationId))
                .andExpect(status().isNotFound());

        mvc.perform(get("/users/" + ownerId + "/events/" + eventId))
                .andExpect(status().isOk());
    }

    @Test
    void category_shouldNotBeDeleted_whenItHasEvents() throws Exception {
        long ownerId = createUser("Иван Петров", "ivan@practicummail.ru");
        long categoryId = createCategory("Концерты");
        createEvent(ownerId, categoryId, 0, true, "Концерт группы");

        mvc.perform(delete("/admin/categories/" + categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("The category is not empty")));
    }

    private long createUser(String name, String email) throws Exception {
        MvcResult result = mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(new UserRequest(name, email)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private long createCategory(String name) throws Exception {
        MvcResult result = mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(new CategoryRequest(name)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private long createEvent(long userId, long categoryId, int participantLimit,
                             boolean requestModeration, String title) throws Exception {
        NewEventRequest request = new NewEventRequest();
        request.setAnnotation("Аннотация события: " + title);
        request.setCategory(categoryId);
        request.setDescription("Описание события: " + title);
        request.setEventDate(LocalDateTime.now().plusDays(2));
        request.setLocation(new LocationDto(55.75f, 37.62f));
        request.setPaid(false);
        request.setParticipantLimit(participantLimit);
        request.setRequestModeration(requestModeration);
        request.setTitle(title);

        MvcResult result = mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state", is("PENDING")))
                .andReturn();
        return extractId(result);
    }

    private void publishEvent(long eventId) throws Exception {
        mvc.perform(patch("/admin/events/" + eventId)
                        .content("{\"stateAction\": \"PUBLISH_EVENT\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("PUBLISHED")));
    }

    private long extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}