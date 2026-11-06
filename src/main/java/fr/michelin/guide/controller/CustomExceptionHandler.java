package fr.michelin.guide.controller;

import fr.michelin.guide.dto.ErreurDto;
import fr.michelin.guide.exception.ForbiddenException;
import fr.michelin.guide.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.security.Principal;
import java.util.stream.Collectors;

// Transforme les erreurs de l'API en réponse { code, message }
@Slf4j
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    // Restaurant ou évaluation introuvable -> 404
    @ExceptionHandler
    public ResponseEntity<ErreurDto> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErreurDto(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // Action interdite pour cet utilisateur (ex : supprimer l'évaluation d'un autre) -> 403
    @ExceptionHandler
    public ResponseEntity<ErreurDto> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErreurDto(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    // Accès refusé par @PreAuthorize : 401 si personne n'est connecté, 403 si le rôle ne suffit pas
    @ExceptionHandler
    public ResponseEntity<ErreurDto> handleAccesRefuse(AccessDeniedException ex, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErreurDto(HttpStatus.UNAUTHORIZED.value(), "Vous devez être connecté"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErreurDto(HttpStatus.FORBIDDEN.value(), "Vous n'avez pas les droits pour cette action"));
    }

    // Données invalides (@Valid) -> 400 avec les messages des champs en erreur
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErreurDto(HttpStatus.BAD_REQUEST.value(), message));
    }

    // Autres erreurs gérées par Spring (JSON mal formé, route inconnue, méthode non autorisée...)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        return ResponseEntity.status(statusCode)
                .body(new ErreurDto(statusCode.value(), ex.getMessage()));
    }

    // Erreur imprévue -> 500, le détail va dans les logs mais n'est pas renvoyé au client
    @ExceptionHandler
    public ResponseEntity<ErreurDto> handleErreurInterne(Exception ex) {
        log.error("Erreur interne", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErreurDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur"));
    }
}
