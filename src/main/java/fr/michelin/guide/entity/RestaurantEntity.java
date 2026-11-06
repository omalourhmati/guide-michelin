package fr.michelin.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * Table "restaurant" : un restaurant du guide.
 * @Getter/@Setter plutôt que @Data : restaurant et évaluations se référencent
 * mutuellement, le toString()/hashCode() de @Data tournerait en boucle.
 */
@Entity
@Table(name = "restaurant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantEntity {

    // Identifiant unique, généré automatiquement par la base
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom du restaurant (90 caractères maximum)
    @Column(length = 90, nullable = false)
    private String nom;

    // Adresse du restaurant (255 caractères maximum)
    @Column(length = 255, nullable = false)
    private String adresse;

    // Clé de l'image dans le bucket S3 "restaurant" (vide tant qu'aucune image n'a été envoyée)
    private String image;

    // Un restaurant possède plusieurs évaluations.
    // mappedBy = "restaurant" : c'est le champ "restaurant" de EvaluationEntity qui porte la clé étrangère
    @OneToMany(mappedBy = "restaurant")
    @Builder.Default
    private List<EvaluationEntity> evaluations = new ArrayList<>();
}
