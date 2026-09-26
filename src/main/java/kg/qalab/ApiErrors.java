package kg.qalab;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    public static Api.Error body(int status,String code,String message,Map<String,String> fields,HttpServletRequest request) {
        return new Api.Error(status,code,message,fields,String.valueOf(request.getAttribute("requestId")),Instant.now());
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Api.Error> business(ApiException e,HttpServletRequest request) {
        return ResponseEntity.status(e.status).body(body(e.status,e.code,e.getMessage(),Map.of(),request));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Api.Error> validation(MethodArgumentNotValidException e,HttpServletRequest request) {
        Map<String,String> fields=new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(body(400,"VALIDATION_ERROR","Проверьте поля запроса",fields,request));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<Api.Error> malformed(Exception e,HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body(400,"MALFORMED_REQUEST","Некорректный JSON, неизвестное поле или неверный тип значения",Map.of(),request));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Api.Error> media(Exception e,HttpServletRequest request) {
        return ResponseEntity.status(415).body(body(415,"UNSUPPORTED_MEDIA_TYPE","Используйте Content-Type: application/json",Map.of(),request));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Api.Error> method(HttpRequestMethodNotSupportedException e,HttpServletRequest request) {
        return ResponseEntity.status(405).header("Allow",String.join(", ",Objects.requireNonNullElse(e.getSupportedMethods(),new String[0])))
            .body(body(405,"METHOD_NOT_ALLOWED","Этот путь не поддерживает выбранный метод",Map.of(),request));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Api.Error> missing(Exception e,HttpServletRequest request) {
        return ResponseEntity.status(404).body(body(404,"NOT_FOUND","Такого пути нет",Map.of(),request));
    }
}
