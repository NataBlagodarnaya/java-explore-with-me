package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.NewEventRequest;
import ru.practicum.ewm.main.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {

    EventFullDto addEvent(Long userId, NewEventRequest request);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);
}