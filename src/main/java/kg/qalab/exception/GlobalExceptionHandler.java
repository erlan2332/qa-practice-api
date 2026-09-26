package kg.qalab.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> business(
        ApiException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(exception.getStatus())
            .body(ApiErrorResponse.of(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                Map.of(),
                request
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fields = new TreeMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                fields.put(error.getField(), error.getDefaultMessage())
            );

        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of(
                400,
                "VALIDATION_ERROR",
                "Проверьте поля запроса",
                fields,
                request
            )
        );
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> malformed(
        Exception exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of(
                400,
                "MALFORMED_REQUEST",
                "Некорректный JSON, неизвестное поле или неверный тип значения",
                Map.of(),
                request
            )
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> mediaType(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(415)
            .body(ApiErrorResponse.of(
                415,
                "UNSUPPORTED_MEDIA_TYPE",
                "Используйте Content-Type: application/json",
                Map.of(),
                request
            ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> method(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        String[] supportedMethods =
            Objects.requireNonNullElse(
                exception.getSupportedMethods(),
                new String[0]
            );

        return ResponseEntity
            .status(405)
            .header("Allow", String.join(", ", supportedMethods))
            .body(ApiErrorResponse.of(
                405,
                "METHOD_NOT_ALLOWED",
                "Этот путь не поддерживает выбранный метод",
                Map.of(),
                request
            ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> missing(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(404)
            .body(ApiErrorResponse.of(
                404,
                "NOT_FOUND",
                "Такого пути нет",
                Map.of(),
                request
            ));
    }
}
