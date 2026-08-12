package com.example._7_jwt.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Lab 7 (TaskFlow) — centralised RFC 7807 error rendering, identical house style to the earlier
 * labs. Note: authentication/authorization failures for <em>protected</em> endpoints (anonymous or
 * invalid-token → 401/403) are produced by the Spring Security filter chain BEFORE a request
 * reaches a controller, so they are not handled here. The two 401s below are the exception: the
 * JWT {@code /api/auth/login} and {@code /api/auth/refresh} endpoints authenticate <em>inside</em>
 * the controller (via the AuthenticationManager and the refresh-token store), so their failures
 * surface here as ordinary exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 401 UNAUTHORIZED — bad credentials at POST /api/auth/login (thrown by the AuthenticationManager)
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        pd.setTitle("Authentication failed");
        return pd;
    }

    // 401 UNAUTHORIZED — unknown/revoked/expired refresh token at POST /api/auth/refresh
    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Invalid token");
        return pd;
    }

    // 404 NOT FOUND
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        return pd;
    }

    // 409 CONFLICT — unique field clash (username / email)
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Duplicate value");
        return pd;
    }

    // 400 BAD REQUEST — bean validation failed (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    // 400 BAD REQUEST — wrong query/path type, e.g. ?status=BOGUS or GET /api/tasks/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value");
        pd.setTitle("Invalid parameter");
        return pd;
    }

    // 500 INTERNAL SERVER ERROR — safety net
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal server error");
        return pd;
    }
}
