package com.portfolens.workflow.controller;

import com.portfolens.workflow.exception.IdempotencyConflictException;
import com.portfolens.workflow.exception.InvalidStepStateException;
import com.portfolens.workflow.exception.InvalidWorkflowStateException;
import com.portfolens.workflow.exception.OptimisticLockConflictException;
import com.portfolens.workflow.exception.WorkflowNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class WorkflowExceptionHandler {
    @ExceptionHandler(WorkflowNotFoundException.class) ResponseEntity<ApiError> notFound(WorkflowNotFoundException ex) { return response(HttpStatus.NOT_FOUND, "WORKFLOW_NOT_FOUND", "Workflow was not found", Map.of()); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ApiError> forbidden(AccessDeniedException ex) { return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access to this workflow is denied", Map.of()); }
    @ExceptionHandler(IdempotencyConflictException.class) ResponseEntity<ApiError> idempotency(IdempotencyConflictException ex) { return response(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage(), Map.of()); }
    @ExceptionHandler({InvalidWorkflowStateException.class, InvalidStepStateException.class}) ResponseEntity<ApiError> invalidState(RuntimeException ex) { return response(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage(), Map.of()); }
    @ExceptionHandler({OptimisticLockConflictException.class, org.springframework.dao.OptimisticLockingFailureException.class}) ResponseEntity<ApiError> optimistic(RuntimeException ex) { return response(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "The workflow changed concurrently; reload and retry", Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", errors);
    }
    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class}) ResponseEntity<ApiError> requestValidation(Exception ex) { return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request parameters or JSON are invalid", Map.of()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class}) ResponseEntity<ApiError> badRequest(Exception ex) { return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request parameters are invalid", Map.of()); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ApiError> integrity() { return response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "A conflicting workflow or step already exists", Map.of()); }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", Map.of()); }
    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, errors));
    }
}
