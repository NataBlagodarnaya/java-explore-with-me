package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateDto;
import ru.practicum.ewm.main.dto.eventrequest.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.eventrequest.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateDto updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}