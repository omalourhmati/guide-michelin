package fr.michelin.guide.dto;

import fr.michelin.guide.entity.EvaluationEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Données d'une évaluation échangées en JSON (validées avec @Valid dans le contrôleur)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDto {

    // Identifiant généré par la base
    private Long id;

    // Pris dans le token de l'utilisateur connecté, pas envoyé par le client
    private String evaluateur;

    // Commentaire obligatoire, 255 caractères maximum
    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(max = 255, message = "Le commentaire ne doit pas dépasser 255 caractères")
    private String commentaire;

    // Nombre d'étoiles recommandé : 0, 1, 2 ou 3
    @NotNull(message = "La note est obligatoire")
    @Min(value = 0, message = "La note doit être comprise entre 0 et 3")
    @Max(value = 3, message = "La note doit être comprise entre 0 et 3")
    private Integer note;

    // Clés des photos des plats dans le bucket S3 "evaluation"
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    // Restaurant concerné par l'évaluation
    private Long restaurantId;

    // Conversion entité -> DTO
    public static EvaluationDto fromEntity(EvaluationEntity entity) {
        return EvaluationDto.builder()
                .id(entity.getId())
                .evaluateur(entity.getEvaluateur())
                .commentaire(entity.getCommentaire())
                .note(entity.getNote())
                .photos(entity.getPhotos())
                .restaurantId(entity.getRestaurant().getId())
                .build();
    }
}
