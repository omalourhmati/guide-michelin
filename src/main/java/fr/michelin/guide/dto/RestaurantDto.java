package fr.michelin.guide.dto;

import fr.michelin.guide.entity.RestaurantEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/*
 * Données d'un restaurant échangées en JSON avec le client.
 * À la création / mise à jour, seuls "nom" et "adresse" sont utilisés et validés.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDto {

    // Identifiant généré par la base
    private Long id;

    // Nom obligatoire, 90 caractères maximum
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 90, message = "Le nom ne doit pas dépasser 90 caractères")
    private String nom;

    // Adresse obligatoire, 255 caractères maximum
    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String adresse;

    // Clé de l'image dans le bucket S3 "restaurant"
    private String image;

    // Évaluations du restaurant
    @Builder.Default
    private List<EvaluationDto> evaluations = new ArrayList<>();

    // Moyenne des notes (-1 si aucune évaluation), calculée par le service
    private Double moyenne;

    // Conversion entité -> DTO, la moyenne est calculée par le service
    public static RestaurantDto fromEntity(RestaurantEntity entity, Double moyenne) {
        return RestaurantDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .adresse(entity.getAdresse())
                .image(entity.getImage())
                .evaluations(entity.getEvaluations().stream()
                        .map(EvaluationDto::fromEntity)
                        .toList())
                .moyenne(moyenne)
                .build();
    }
}
