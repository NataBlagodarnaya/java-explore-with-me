package ru.practicum.ewm.main.mapper;

import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.model.ParticipationRequest;

public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}