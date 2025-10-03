package com.wealthsearch.web.error;

import com.wealthsearch.model.exception.*;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErrorEntry>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ErrorEntry> errors = mergeBindingErrors(exception.getBindingResult());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(fallbackIfEmpty(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<List<ErrorEntry>> handleConstraintViolation(ConstraintViolationException exception) {
        List<ErrorEntry> errors = exception.getConstraintViolations()
                                           .stream()
                                           .map(violation -> new ErrorEntry(
                                                   ErrorMessage.CONSTRAINT_VIOLATION.format(violation.getPropertyPath(),
                                                                                            violation.getMessage())))
                                           .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(fallbackIfEmpty(errors));
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<List<ErrorEntry>> handleClientAlreadyExists(ClientAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(List.of(new ErrorEntry(exception.getMessage())));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<List<ErrorEntry>> handleEntityAlreadyExists(EntityAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(List.of(new ErrorEntry(exception.getMessage())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<List<ErrorEntry>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                             .body(List.of(new ErrorEntry(exception.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<ErrorEntry>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(List.of(new ErrorEntry("Malformed JSON request")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<List<ErrorEntry>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(List.of(new ErrorEntry(
                                     ErrorMessage.FIELD_VALIDATION_ERROR.format(exception.getParameterName(),
                                                                                "required parameter is missing"))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<ErrorEntry>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(List.of(new ErrorEntry(
                                     ErrorMessage.FIELD_VALIDATION_ERROR.format(exception.getName(), "invalid value: "
                                             + exception.getValue()))));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<List<ErrorEntry>> handleBadRequest(BadRequestException exception) {
        log.error("Bad request: ", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(fallbackIfEmpty(exception.getErrors()));
    }

    @ExceptionHandler(OllamaClientException.class)
    public ResponseEntity<List<ErrorEntry>> handleOllamaClientException(OllamaClientException exception) {
        log.error("Ollama service error: ", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                             .body(List.of(new ErrorEntry(
                                     ErrorMessage.OLLAMA_SERVICE_ERROR.format(exception.getMessage()))));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<List<ErrorEntry>> handleNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body(List.of(new ErrorEntry("Unknown endpoint")));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<List<ErrorEntry>> handleBusinessNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body(exception.getErrors());
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<List<ErrorEntry>> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(exception.getErrors());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<ErrorEntry>> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(List.of(new ErrorEntry(ErrorMessage.INTERNAL_ERROR.message())));
    }

    private List<ErrorEntry> mergeBindingErrors(BindingResult bindingResult) {
        return Stream.concat(bindingResult.getFieldErrors()
                                          .stream()
                                          .map(error -> new ErrorEntry(
                                                  ErrorMessage.FIELD_VALIDATION_ERROR.format(error.getField(),
                                                                                             error.getDefaultMessage()))),
                             bindingResult.getGlobalErrors()
                                          .stream()
                                          .map(error -> new ErrorEntry(
                                                  ErrorMessage.GLOBAL_VALIDATION_ERROR.format(error.getDefaultMessage()))))
                     .toList();
    }

    private List<ErrorEntry> fallbackIfEmpty(List<ErrorEntry> errors) {
        return errors.isEmpty() ? List.of(new ErrorEntry(ErrorMessage.INTERNAL_ERROR.message())) : errors;
    }
}
