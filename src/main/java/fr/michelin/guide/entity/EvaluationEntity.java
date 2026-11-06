package fr.michelin.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * Table "evaluation" : l'avis d'un évaluateur sur un restaurant.
 * Même choix que RestaurantEntity : @Getter/@Setter et pas @Data (relation dans les deux sens).
 */
@Entity
@Table(name = "evaluation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationEntity {

    // Identifiant unique, généré automatiquement par la base
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom de l'évaluateur (50 caractères maximum), récupéré depuis le token
    @Column(length = 50, nullable = false)
    private String evaluateur;

    // Commentaire de l'évaluateur (255 caractères maximum)
    @Column(length = 255, nullable = false)
    private String commentaire;

    // Nombre d'étoiles recommandé : 0, 1, 2 ou 3
    @Column(nullable = false)
    private Integer note;

    // Clés des photos des plats dans le bucket S3 "evaluation"
    @ElementCollection
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    // Plusieurs évaluations appartiennent au même restaurant
    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurant;
}
