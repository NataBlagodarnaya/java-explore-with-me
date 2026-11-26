package ru.practicum.ewm.main.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.eventrequest.ParticipationRequestDto;
import ru.practicum.ewm.main.model.ParticipationRequest;

@UtilityClass
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