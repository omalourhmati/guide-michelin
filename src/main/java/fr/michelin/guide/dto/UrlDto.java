package fr.michelin.guide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Lien présigné renvoyé au client pour envoyer ou afficher une image sur le S3
@Data
@AllArgsConstructor
public class UrlDto {

    private String url;
}
