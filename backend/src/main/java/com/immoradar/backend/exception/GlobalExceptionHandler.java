package com.immoradar.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global standardisé selon la norme RFC 7807 (Problem Details).
 * Retourne des réponses au format application/problem+json typées et exploitables côté client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Un ou plusieurs champs du deal contiennent des valeurs invalides."
        );
        problemDetail.setTitle("Validation Error (RFC 7807)");
        problemDetail.setType(URI.create("https://immoradar.fr/errors/validation-failed"));
        problemDetail.setProperty("timestamp", Instant.now());

        Map<String, String> invalidParams = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            invalidParams.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("invalidParams", invalidParams);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Requête Invalide");
        problemDetail.setType(URI.create("https://immoradar.fr/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne imprévue est survenue sur le serveur."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://immoradar.fr/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
