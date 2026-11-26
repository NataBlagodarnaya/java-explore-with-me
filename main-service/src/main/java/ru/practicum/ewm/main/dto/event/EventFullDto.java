package ru.practicum.ewm.main.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.model.EventState;
import ru.practicum.ewm.main.util.DateTimeConstants;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFullDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN)
    private LocalDateTime createdOn;

    private String description;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    private UserShortDto initiator;
    private LocationDto location;
    private boolean paid;
    private int participantLimit;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN)
    private LocalDateTime publishedOn;

    private boolean requestModeration;
    private EventState state;
    private String title;
    private Long views;
}