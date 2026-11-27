package ru.practicum.ewm.main.controller.priv;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.service.SubscriptionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
@Validated
public class PrivateSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public UserShortDto subscribe(@PathVariable Long userId,
                                  @PathVariable Long authorId) {
        log.info("Подписка пользователя {} на автора {}", userId, authorId);
        return subscriptionService.subscribe(userId, authorId);
    }

    @DeleteMapping("/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long userId,
                            @PathVariable Long authorId) {
        log.info("Отписка пользователя {} от автора {}", userId, authorId);
        subscriptionService.unsubscribe(userId, authorId);
    }

    @GetMapping
    public List<UserShortDto> getSubscriptions(@PathVariable Long userId) {
        log.info("Получение подписок пользователя {}", userId);
        return subscriptionService.getSubscriptions(userId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getSubscriptionEvents(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                     @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получение событий авторов, на которых подписан пользователь {}, from={}, size={}",
                userId, from, size);
        return subscriptionService.getSubscriptionEvents(userId, from, size);
    }
}