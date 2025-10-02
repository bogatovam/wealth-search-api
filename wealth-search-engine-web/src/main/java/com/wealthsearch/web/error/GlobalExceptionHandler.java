package com.wealthsearch.web.error;

import com.wealthsearch.api.exception.ClientAlreadyExistsException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiError> errors = mergeBindingErrors(exception.getBindingResult());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(fallbackIfEmpty(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<List<ApiError>> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiError> errors = exception.getConstraintViolations()
                                         .stream()
                                         .map(violation -> new ApiError(
                                                 ErrorMessage.CONSTRAINT_VIOLATION.format(violation.getPropertyPath(),
                                                                                          violation.getMessage())))
                                         .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(fallbackIfEmpty(errors));
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<List<ApiError>> handleClientAlreadyExists(ClientAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(List.of(new ApiError(exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<ApiError>> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(List.of(new ApiError(ErrorMessage.INTERNAL_ERROR.message())));
    }

    private List<ApiError> mergeBindingErrors(BindingResult bindingResult) {
        return Stream.concat(bindingResult.getFieldErrors()
                                          .stream()
                                          .map(error -> new ApiError(
                                                  ErrorMessage.FIELD_VALIDATION_ERROR.format(error.getField(),
                                                                                             error.getDefaultMessage()))),
                             bindingResult.getGlobalErrors()
                                          .stream()
                                          .map(error -> new ApiError(
                                                  ErrorMessage.GLOBAL_VALIDATION_ERROR.format(error.getDefaultMessage()))))
                     .toList();
    }

    private List<ApiError> fallbackIfEmpty(List<ApiError> errors) {
        return errors.isEmpty() ? List.of(new ApiError(ErrorMessage.INTERNAL_ERROR.message())) : errors;
    }
}
