package fr.michelin.guide.repository;

import fr.michelin.guide.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Accès à la table "evaluation" (méthodes de base fournies par JpaRepository)
@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    // Toutes les évaluations écrites par un évaluateur (route "mes évaluations")
    List<EvaluationEntity> findByEvaluateur(String evaluateur);
}
