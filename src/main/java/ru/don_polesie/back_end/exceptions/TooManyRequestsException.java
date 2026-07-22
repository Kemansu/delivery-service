package ru.don_polesie.back_end.exceptions;

/**
 * Слишком много попыток (вход заблокирован после серии неудач). Маппится в 429.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
