package fr.michelin.guide.exception;

// Levée quand un restaurant ou une évaluation demandé n'existe pas (renvoyée en 404)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
