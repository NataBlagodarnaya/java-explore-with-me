package ru.practicum.ewm.main.controller.priv;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateDto;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateEventRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("Получение заявок на участие в событии {} пользователя {}", eventId, userId);
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateDto updateRequestStatus(@PathVariable Long userId,
                                                           @PathVariable Long eventId,
                                                           @Valid @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("Изменение статуса заявок на участие в событии {} пользователем {}", eventId, userId);
        return requestService.updateRequestStatus(userId, eventId, request);
    }
}