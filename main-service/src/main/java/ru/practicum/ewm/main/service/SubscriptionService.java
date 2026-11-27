package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;

import java.util.List;

public interface SubscriptionService {

    UserShortDto subscribe(Long userId, Long authorId);

    void unsubscribe(Long userId, Long authorId);

    List<UserShortDto> getSubscriptions(Long userId);

    List<EventShortDto> getSubscriptionEvents(Long userId, int from, int size);
}