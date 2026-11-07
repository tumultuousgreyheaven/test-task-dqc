package com.example.dqc.api;

import com.example.dqc.api.dto.ErrorResponse;
import com.example.dqc.application.exception.ApplicationException;
import com.example.dqc.application.exception.ConflictException;
import com.example.dqc.application.exception.ErrorCode;
import com.example.dqc.application.exception.InvalidRequestException;
import com.example.dqc.application.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Приводит все ошибки к единому формату {@code {"error": {"code": "...", "message": "..."}}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Ожидаемые ошибки сервисов: 404, 422, 409. */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplication(ApplicationException e) {
        return respond(statusOf(e), e.code().name(), e.getMessage());
    }

    /** Параметр или тело, которые Spring не смог разобрать: limit=abc, snapshot_date=вчера. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_PARAMETER.name(),
                "Недопустимое значение параметра %s: %s".formatted(e.getName(), e.getValue()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_PARAMETER.name(),
                "Не указан параметр " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INVALID_PARAMETER.name(),
                "Не удалось разобрать тело запроса, ожидается JSON вида {\"snapshot_date\": \"2026-10-26\"}");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Нет такого адреса: " + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Метод не поддерживается: " + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Ожидается Content-Type: application/json");
    }

    /** Всё непредвиденное: подробности только в лог, клиенту общий текст. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Необработанная ошибка", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервиса");
    }

    private static HttpStatus statusOf(ApplicationException e) {
        if (e instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof InvalidRequestException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (e instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
