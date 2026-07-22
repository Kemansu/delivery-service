package ru.don_polesie.back_end.exceptions.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import ru.don_polesie.back_end.exceptions.*;

@Slf4j
@ControllerAdvice
public class ExceptionHandlerService {

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Information> handleObjectNotFoundException(ObjectNotFoundException ex, WebRequest request) {
        log.warn("Object not found - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new Information("Объект не найден.", ex.getMessage())
        );
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<Information> handleRequestValidationException(RequestValidationException ex, WebRequest request) {
        log.warn("Validation error - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new Information("Некорректный запрос.", ex.getMessage())
        );
    }

    @ExceptionHandler(ConflictDataException.class)
    public ResponseEntity<Information> handleConflictDataException(ConflictDataException ex, WebRequest request) {
        log.warn("Data conflict - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new Information("Конфликт в данных.", ex.getMessage())
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Information> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.warn("Forbidden - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new Information("Доступ запрещен.", ex.getMessage())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Information> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new Information("У вас недостаточно прав для этого действия.", ex.getMessage())
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Information> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new Information("Ошибка аутентификации", "Неверные учетные данные")
        );
    }


    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Information> handleTooManyRequestsException(TooManyRequestsException ex, WebRequest request) {
        log.warn("Too many requests - URL: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                new Information(ex.getMessage(), "Too many login attempts")
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Information> handleException(Exception ex, WebRequest request) {
        log.error("Internal error - URL: {}, Error: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new Information("Возникла внутренняя ошибка сервера.", ex.getMessage())
        );
    }
}
