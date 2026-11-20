package ru.practicum.ewm.main.mapper;

import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.LocationDto;
import ru.practicum.ewm.main.dto.NewEventRequest;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;

import java.time.LocalDateTime;

public class EventMapper {

    public static Event toEvent(NewEventRequest request, Category category, User initiator) {
        return Event.builder()
                .title(request.getTitle())
                .annotation(request.getAnnotation())
                .description(request.getDescription())
                .category(category)
                .initiator(initiator)
                .eventDate(request.getEventDate())
                .createdOn(LocalDateTime.now())
                .location(toLocation(request.getLocation()))
                .paid(Boolean.TRUE.equals(request.getPaid()))
                .participantLimit(request.getParticipantLimit() == null ? 0 : request.getParticipantLimit())
                .requestModeration(!Boolean.FALSE.equals(request.getRequestModeration()))
                .state(EventState.PENDING)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event, long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(toUserShortDto(event.getInitiator()))
                .location(toLocationDto(event.getLocation()))
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.isRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static EventShortDto toEventShortDto(Event event, long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(toUserShortDto(event.getInitiator()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static Location toLocation(LocationDto dto) {
        return new Location(dto.getLat(), dto.getLon());
    }

    public static LocationDto toLocationDto(Location location) {
        return new LocationDto(location.getLat(), location.getLon());
    }

    private static UserShortDto toUserShortDto(User user) {
        return new UserShortDto(user.getId(), user.getName());
    }
}