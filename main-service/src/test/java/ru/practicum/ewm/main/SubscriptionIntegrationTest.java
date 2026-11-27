package ru.practicum.ewm.main;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.repository.SubscriptionRepository;
import ru.practicum.ewm.main.util.DateTimeConstants;
import ru.practicum.ewm.stats.client.StatsClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private StatsClient statsClient;

    @Test
    void subscribe_shouldCreateSubscriptionAndUnsubscribeShouldRemoveIt() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");

        mockMvc.perform(post("/users/" + subscriber + "/subscriptions/" + author))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(author))
                .andExpect(jsonPath("$.name").value("Анна Иванова"));

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(author));

        mockMvc.perform(delete("/users/" + subscriber + "/subscriptions/" + author))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void subscribe_shouldReturnConflict_whenSubscriptionAlreadyExists() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");
        subscribe(subscriber, author);

        mockMvc.perform(post("/users/" + subscriber + "/subscriptions/" + author))
                .andExpect(status().isConflict());
    }

    @Test
    void subscribe_shouldReturnConflict_whenUserSubscribesToHimself() throws Exception {
        long user = createUser("Иван Петров", "ivan@practicummail.ru");

        mockMvc.perform(post("/users/" + user + "/subscriptions/" + user))
                .andExpect(status().isConflict());
    }

    @Test
    void subscribe_shouldReturnNotFound_whenAuthorDoesNotExist() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");

        mockMvc.perform(post("/users/" + subscriber + "/subscriptions/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsubscribe_shouldReturnNotFound_whenSubscriptionDoesNotExist() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");

        mockMvc.perform(delete("/users/" + subscriber + "/subscriptions/" + author))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscriptionEvents_shouldReturnPublishedEventsOfSubscribedAuthorsSorted() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long authorA = createUser("Анна Иванова", "anna@practicummail.ru");
        long authorB = createUser("Борис Петров", "boris@practicummail.ru");
        long stranger = createUser("Вера Смирнова", "vera@practicummail.ru");
        long category = createCategory("Концерты");
        long later = createEvent(authorA, category, "Концерт Анны поздний", 5);
        long earlier = createEvent(authorA, category, "Концерт Анны ранний", 3);
        long boris = createEvent(authorB, category, "Концерт Бориса", 2);
        long foreign = createEvent(stranger, category, "Концерт Веры", 2);
        createEvent(authorA, category, "Концерт Анны черновик", 4);
        publishEvent(later);
        publishEvent(earlier);
        publishEvent(boris);
        publishEvent(foreign);
        subscribe(subscriber, authorA);
        subscribe(subscriber, authorB);

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Концерт Анны ранний"))
                .andExpect(jsonPath("$[1].title").value("Концерт Анны поздний"))
                .andExpect(jsonPath("$[2].title").value("Концерт Бориса"));
    }

    @Test
    void getSubscriptionEvents_shouldCutPageByFromAndSize() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");
        long category = createCategory("Концерты");
        publishEvent(createEvent(author, category, "Концерт первый", 2));
        publishEvent(createEvent(author, category, "Концерт второй", 3));
        publishEvent(createEvent(author, category, "Концерт третий", 4));
        subscribe(subscriber, author);

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions/events")
                        .param("from", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Концерт второй"));
    }

    @Test
    void getSubscriptionEvents_shouldReturnEmptyList_whenThereAreNoSubscriptions() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteUser_shouldRemoveSubscriptionsOfDeletedAuthor() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");
        subscribe(subscriber, author);

        mockMvc.perform(delete("/admin/users/" + author))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + subscriber + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteUser_shouldRemoveSubscriptionsOfDeletedSubscriber() throws Exception {
        long subscriber = createUser("Иван Петров", "ivan@practicummail.ru");
        long author = createUser("Анна Иванова", "anna@practicummail.ru");
        subscribe(subscriber, author);

        mockMvc.perform(delete("/admin/users/" + subscriber))
                .andExpect(status().isNoContent());

        subscriptionRepository.flush();
        assertEquals(0, subscriptionRepository.count());
    }

    private void subscribe(long subscriber, long author) throws Exception {
        mockMvc.perform(post("/users/" + subscriber + "/subscriptions/" + author))
                .andExpect(status().isCreated());
    }

    private long createUser(String name, String email) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}";
        return readId(mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createCategory(String name) throws Exception {
        String body = "{\"name\":\"" + name + "\"}";
        return readId(mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createEvent(long userId, long categoryId, String title, int daysAhead) throws Exception {
        String eventDate = LocalDateTime.now().plusDays(daysAhead)
                .format(DateTimeFormatter.ofPattern(DateTimeConstants.DATE_TIME_PATTERN));
        String body = "{\"annotation\":\"Живой концерт под открытым небом\","
                + "\"category\":" + categoryId + ","
                + "\"description\":\"Большой концерт на открытой площадке города\","
                + "\"eventDate\":\"" + eventDate + "\","
                + "\"location\":{\"lat\":55.75,\"lon\":37.62},"
                + "\"paid\":false,"
                + "\"participantLimit\":0,"
                + "\"requestModeration\":true,"
                + "\"title\":\"" + title + "\"}";
        return readId(mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void publishEvent(long eventId) throws Exception {
        mockMvc.perform(patch("/admin/events/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk());
    }

    private long readId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }
}