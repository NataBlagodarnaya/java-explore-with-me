package ru.practicum.ewm.main.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import ru.practicum.ewm.main.util.DateTimeConstants;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private final List<String> errors;
    private final String message;
    private final String reason;
    private final String status;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME_PATTERN)
    private final LocalDateTime timestamp;
}