package com.example.PhotoEpilepsyAnalyzer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Catches errors that happen outside a controller method's own try/catch
 * (oversized uploads, missing form fields, etc.) and converts them into the
 * same AnalysisResponse JSON shape the frontend expects, instead of letting
 * Spring fall back to its default HTML Whitelabel error page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<AnalysisResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new AnalysisResponse(false, null,
                        "That file is larger than the server currently allows. " +
                                "Increase spring.servlet.multipart.max-file-size if this is expected."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<AnalysisResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest()
                .body(new AnalysisResponse(false, null,
                        "Expected a form field named '" + ex.getRequestPartName() + "' but it was missing."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AnalysisResponse> handleAnythingElse(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new AnalysisResponse(false, null,
                        "Unexpected server error: " + ex.getMessage()));
    }
}