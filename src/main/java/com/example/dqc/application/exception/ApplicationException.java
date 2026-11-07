package com.example.dqc.application.exception;

import java.util.Objects;

/**
 * Ожидаемая ошибка прикладного уровня: неверный запрос или обращение к несуществующему объекту.
 * Сообщение предназначено для клиента API. Соответствие HTTP-статусам задаёт слой api по подклассу.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    protected ApplicationException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
