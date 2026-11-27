package ru.practicum.ewm.main.controller.priv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.SubscriptionService;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateSubscriptionController.class)
class PrivateSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    void subscribe_shouldReturnCreatedAndAuthor() throws Exception {
        when(subscriptionService.subscribe(1L, 2L)).thenReturn(new UserShortDto(2L, "Мария Сидорова"));

        mockMvc.perform(post("/users/1/subscriptions/2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Мария Сидорова"));
    }

    @Test
    void subscribe_shouldReturnConflict_whenServiceThrowsConflict() throws Exception {
        when(subscriptionService.subscribe(1L, 1L))
                .thenThrow(new ConflictException("User cannot subscribe to himself"));

        mockMvc.perform(post("/users/1/subscriptions/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void subscribe_shouldReturnNotFound_whenServiceThrowsNotFound() throws Exception {
        when(subscriptionService.subscribe(1L, 99L))
                .thenThrow(new NotFoundException("User with id=99 was not found"));

        mockMvc.perform(post("/users/1/subscriptions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsubscribe_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/1/subscriptions/2"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).unsubscribe(1L, 2L);
    }

    @Test
    void unsubscribe_shouldReturnNotFound_whenSubscriptionDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Subscription was not found"))
                .when(subscriptionService).unsubscribe(1L, 2L);

        mockMvc.perform(delete("/users/1/subscriptions/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscriptions_shouldReturnAuthors() throws Exception {
        when(subscriptionService.getSubscriptions(1L)).thenReturn(List.of(
                new UserShortDto(2L, "Мария Сидорова"),
                new UserShortDto(3L, "Пётр Иванов")));

        mockMvc.perform(get("/users/1/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Пётр Иванов"));
    }

    @Test
    void getSubscriptions_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(subscriptionService.getSubscriptions(99L))
                .thenThrow(new NotFoundException("User with id=99 was not found"));

        mockMvc.perform(get("/users/99/subscriptions"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscriptionEvents_shouldUseDefaultFromAndSize() throws Exception {
        when(subscriptionService.getSubscriptionEvents(1L, 0, 10)).thenReturn(List.of(
                EventShortDto.builder().id(10L).title("Концерт группы").build()));

        mockMvc.perform(get("/users/1/subscriptions/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Концерт группы"));

        verify(subscriptionService).getSubscriptionEvents(1L, 0, 10);
    }

    @Test
    void getSubscriptionEvents_shouldPassFromAndSize() throws Exception {
        when(subscriptionService.getSubscriptionEvents(1L, 5, 3)).thenReturn(List.of());

        mockMvc.perform(get("/users/1/subscriptions/events")
                        .param("from", "5")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(subscriptionService).getSubscriptionEvents(1L, 5, 3);
    }

    @Test
    void getSubscriptionEvents_shouldReturnBadRequest_whenFromIsNegative() throws Exception {
        mockMvc.perform(get("/users/1/subscriptions/events").param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(subscriptionService, never()).getSubscriptionEvents(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getSubscriptionEvents_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mockMvc.perform(get("/users/1/subscriptions/events").param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(subscriptionService, never()).getSubscriptionEvents(anyLong(), anyInt(), anyInt());
    }
}