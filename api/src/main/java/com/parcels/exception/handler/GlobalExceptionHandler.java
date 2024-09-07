package com.parcels.exception.handler;

import com.parcels.exception.ErrorDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorDetails> handleResponseStatusException(HttpServletRequest request,
                                                                    ResponseStatusException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, ex, ex.getClass(), ex.getReason(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(ex.getStatusCode().value()).body(errorDetails);
    log.error("Caught response status exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleMethodArgumentNotValidException(HttpServletRequest request,
                                                                            MethodArgumentNotValidException ex) {
    String exceptionMessage = Optional.ofNullable(ex.getDetailMessageArguments()).map(Arrays::toString).orElse(ex.getMessage());
    final ErrorDetails errorDetails = ErrorDetails.of(request, ex, ex.getClass(), exceptionMessage, rootCause(ex));
    errorDetails.property("problemDetails", ex.getBody());
    errorDetails.property("parameter", Objects.toString(ex.getParameter()));
    errorDetails.property("errorCount", ex.getErrorCount());
    errorDetails.property("errors", ex.getAllErrors().toString());
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(ex.getStatusCode().value()).body(errorDetails);
    log.error("Caught response status exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorDetails> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException ex) {
    String body;
    try {
      body = IOUtils.toString(ex.getHttpInputMessage().getBody(), request.getCharacterEncoding());
    } catch (IOException e) {
      log.warn("couldn't read http input message", e);
      body = "n/a";
    }

    final var errorDetails = ErrorDetails.of(request, BAD_REQUEST, ex.getClass(), ex.getMessage(), rootCause(ex)).property("body", body);
    final var response = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught response status exception => {}", response.getBody(), ex);
    return response;
  }

  @ExceptionHandler(DataAccessException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleDataAccessException(HttpServletRequest request, DataAccessException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, UNPROCESSABLE_ENTITY, ex.getClass(), ex.getMessage(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught data access exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(HttpServletRequest request, DataIntegrityViolationException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, CONFLICT, ex.getClass(), ex.getMessage(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught data integrity violation exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleMissingServletRequestParameterException(HttpServletRequest request, MissingServletRequestParameterException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, BAD_REQUEST, ex.getClass(), ex.getMessage(), rootCause(ex))
        .property("parameterName", ex.getParameterName())
        .property("parameterName", ex.getParameterName());
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught missing servlet request parameter exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler({ValidationException.class, EntityNotFoundException.class})
  @Primary
  public ResponseEntity<ErrorDetails> handleValidationException(HttpServletRequest request, Exception ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, BAD_REQUEST, ex.getClass(), ex.getMessage(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught validation exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleConstraintViolationException(HttpServletRequest request, ConstraintViolationException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, BAD_REQUEST, ex.getClass(), ex.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", ")), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught constraint violation exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(CannotCreateTransactionException.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleCannotCreateTransactionException(HttpServletRequest request, CannotCreateTransactionException ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, SERVICE_UNAVAILABLE, ex.getClass(), ex.getMessage(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught cannot create transaction exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  @ExceptionHandler(Exception.class)
  @Primary
  public ResponseEntity<ErrorDetails> handleException(HttpServletRequest request, Exception ex) {
    final ErrorDetails errorDetails = ErrorDetails.of(request, INTERNAL_SERVER_ERROR, ex.getClass(), ex.getMessage(), rootCause(ex));
    final ResponseEntity<ErrorDetails> errorResponse = ResponseEntity.status(errorDetails.getStatus()).body(errorDetails);
    log.error("Caught exception => {}", errorResponse.getBody(), ex);
    return errorResponse;
  }

  private Throwable rootCause(Throwable throwable) {
    final var rootCause = ExceptionUtils.getRootCause(throwable);
    return rootCause != throwable ? rootCause : null;
  }
}
