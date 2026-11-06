package fr.michelin.guide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Corps renvoyé en cas d'erreur : code HTTP + message explicatif
@Data
@AllArgsConstructor
public class ErreurDto {

    private int code;

    private String message;
}
