package ru.practicum.ewm.stats.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(final ValidationException ex) {
        log.error("Ошибка валидации параметров запроса (Bad Request): {}", ex.getMessage());
        return new ErrorResponse("Ошибка валидации: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArgumentNotValidExceptions(MethodArgumentNotValidException ex) {
        Object target = ex.getBindingResult().getTarget();
        log.error("Ошибка автоматической валидации для объекта: {}", target);

        List<String> details = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.add(fieldName + ":" + errorMessage);
            log.error("Детали ошибки валидации -> Поле '{}': {}", fieldName, errorMessage);
        });

        String finalMessage = "Ошибка валидации полей: " + String.join(", ", details);
        return new ErrorResponse(finalMessage);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameterException(final MissingServletRequestParameterException ex) {
        log.error("Пропущен обязательный параметр запроса: {}", ex.getMessage());
        return new ErrorResponse("Пропущен обязательный параметр: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatchException(final MethodArgumentTypeMismatchException ex) {
        log.error("Неверный тип параметра в запросе: {}", ex.getMessage());
        String msg = String.format("Параметр '%s' должен быть типа %s", ex.getName(), ex.getRequiredType().getSimpleName());
        return new ErrorResponse("Неверный формат параметров запроса: " + msg);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllUncaughtExceptions(final Throwable ex) {
        log.error("Что-то пошло не так. Это необработанная ошибка сервера: {}", ex.getMessage(), ex);
        return new ErrorResponse("Внутренняя ошибка сервера: " + ex.getMessage());
    }
}