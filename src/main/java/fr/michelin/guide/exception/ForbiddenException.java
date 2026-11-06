package fr.michelin.guide.exception;

// Levée quand l'utilisateur connecté n'a pas le droit de faire l'action demandée (renvoyée en 403)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
