package ru.practicum.ewm.main.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final String REASON_BAD_REQUEST = "Incorrectly made request.";
    private static final String REASON_NOT_FOUND = "The required object was not found.";
    private static final String REASON_CONFLICT = "For the requested operation the conditions are not met.";
    private static final String REASON_INTEGRITY = "Integrity constraint has been violated.";
    private static final String REASON_UNEXPECTED = "Unexpected error occurred.";

    private static final String MESSAGE_INTERNAL_ERROR = "Internal server error";
    private static final String FIELD_ERROR_FORMAT = "Field: %s. Error: %s. Value: %s";

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException ex) {
        log.warn("Объект не найден: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, REASON_NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(final ConflictException ex) {
        log.warn("Конфликт: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, REASON_CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIntegrity(final DataIntegrityViolationException ex) {
        log.warn("Нарушение целостности данных: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, REASON_INTEGRITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(final MissingServletRequestParameterException ex) {
        log.warn("Пропущен обязательный параметр: {}", ex.getParameterName());
        return buildError(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.warn("Неверный тип параметра {}: {}", ex.getName(), ex.getValue());
        return buildError(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotValid(final MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format(FIELD_ERROR_FORMAT,
                        e.getField(), e.getDefaultMessage(), e.getRejectedValue()))
                .toList();
        log.warn("Ошибка валидации тела запроса: {}", errors);
        String message = errors.isEmpty() ? ex.getMessage() : errors.get(0);
        return buildError(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, message, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException ex) {
        log.warn("Ошибка валидации параметров: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(final HttpMessageNotReadableException ex) {
        log.warn("Некорректное тело запроса: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, REASON_BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(final Exception ex) {
        log.error("Необработанная ошибка сервера", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, REASON_UNEXPECTED,
                MESSAGE_INTERNAL_ERROR, List.of());
    }

    private ErrorResponse buildError(HttpStatus status, String reason, String message, List<String> errors) {
        return ErrorResponse.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}