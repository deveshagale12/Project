package com.Project;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

/**
 * Centralized exception handling for the whole application.
 * Every controller's exceptions funnel through here so callers always
 * get a consistent, predictable JSON error shape.
 */

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------- Domain specific exceptions ----------

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                    HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // ---------- Validation (e.g. @Valid on @RequestBody) ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields",
                extractPath(request)
        );
        error.setValidationErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ---------- Malformed JSON / unreadable body ----------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    // ---------- Missing request params ----------

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName(), request);
    }

    // ---------- Type mismatch (e.g. /users/abc instead of /users/1) ----------

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // ---------- Wrong HTTP method ----------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                    HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    // ---------- 404 route not found ----------

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "No handler found for this endpoint", request);
    }

    // ---------- DB constraint violations (e.g. race-condition duplicate insert) ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT,
                "A record with the given unique fields (e.g. email/username) already exists", request);
    }

    // ---------- Async/CompletableFuture wrapped exceptions ----------

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponse> handleCompletionException(CompletionException ex,
                                                                     HttpServletRequest request) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof UserAlreadyExistsException) {
            return buildResponse(HttpStatus.CONFLICT, cause.getMessage(), request);
        }
        if (cause instanceof ResourceNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, cause.getMessage(), request);
        }
        if (cause instanceof InvalidCredentialsException) {
            return buildResponse(HttpStatus.UNAUTHORIZED, cause.getMessage(), request);
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong while processing your request", request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Requested resource not found", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ---------- Catch-all fallback for anything unhandled ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception at " + request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // ---------- helpers ----------

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
                                                          HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }

    private String extractPath(WebRequest request) {
        String description = request.getDescription(false); // "uri=/api/..."
        return description.replace("uri=", "");
    }
}
