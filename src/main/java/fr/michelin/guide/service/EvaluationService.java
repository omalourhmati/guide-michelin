package fr.michelin.guide.service;

import fr.michelin.guide.entity.EvaluationEntity;
import fr.michelin.guide.entity.RestaurantEntity;
import fr.michelin.guide.exception.ForbiddenException;
import fr.michelin.guide.exception.NotFoundException;
import fr.michelin.guide.repository.EvaluationRepository;
import fr.michelin.guide.repository.S3Repository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// Logique métier des évaluations (ajout, lecture, suppression, photos, recherche)
@Service
@RequiredArgsConstructor
public class EvaluationService {

    // Bucket S3 qui contient les photos des plats
    private static final String BUCKET = "evaluation";

    private final EvaluationRepository evaluationRepository;

    private final RestaurantService restaurantService;

    private final S3Repository s3Repository;

    private final IndexService indexService;

    // Au démarrage, toutes les évaluations de la base sont ajoutées à l'index de recherche
    @PostConstruct
    public void indexerEvaluations() {
        for (EvaluationEntity evaluation : evaluationRepository.findAll()) {
            indexService.indexEvaluation(evaluation.getId(), evaluation.getCommentaire());
        }
    }

    // Ajoute une évaluation sur un restaurant existant et l'ajoute à l'index de recherche
    public EvaluationEntity createEvaluation(Long restaurantId, String evaluateur, String commentaire, Integer note) {
        RestaurantEntity restaurant = restaurantService.getRestaurantById(restaurantId);
        EvaluationEntity evaluation = EvaluationEntity.builder()
                .evaluateur(evaluateur)
                .commentaire(commentaire)
                .note(note)
                .restaurant(restaurant)
                .build();
        EvaluationEntity evaluationEnregistree = evaluationRepository.save(evaluation);
        indexService.indexEvaluation(evaluationEnregistree.getId(), evaluationEnregistree.getCommentaire());
        return evaluationEnregistree;
    }

    // Récupère une évaluation par son id, ou lève une NotFoundException si elle n'existe pas
    public EvaluationEntity getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("L'évaluation " + id + " n'existe pas"));
    }

    // Toutes les évaluations écrites par un évaluateur
    public List<EvaluationEntity> getEvaluationsByEvaluateur(String evaluateur) {
        return evaluationRepository.findByEvaluateur(evaluateur);
    }

    // Modifie le commentaire et la note d'une évaluation : autorisé pour son auteur ou pour un administrateur
    public EvaluationEntity updateEvaluation(Long id, String commentaire, Integer note, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = getEvaluationById(id);
        verifierAuteurOuAdmin(evaluation, utilisateur, admin);
        evaluation.setCommentaire(commentaire);
        evaluation.setNote(note);
        EvaluationEntity evaluationEnregistree = evaluationRepository.save(evaluation);
        indexService.indexEvaluation(id, commentaire);
        return evaluationEnregistree;
    }

    // Supprime une évaluation : autorisé pour son auteur ou pour un administrateur
    public void deleteEvaluation(Long id, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = getEvaluationById(id);
        verifierAuteurOuAdmin(evaluation, utilisateur, admin);
        evaluationRepository.delete(evaluation);
        indexService.removeEvaluation(id);
    }

    // Évaluations dont le commentaire contient au moins un des mots-clés
    public List<EvaluationEntity> searchEvaluations(List<String> motsCles) {
        List<Long> ids = indexService.searchEvaluations(String.join(" ", motsCles));
        return evaluationRepository.findAllById(ids);
    }

    // Ajoute une photo à l'évaluation et renvoie le lien pour l'envoyer sur le S3 (auteur ou administrateur)
    public String getPhotoUploadUrl(Long id, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = getEvaluationById(id);
        verifierAuteurOuAdmin(evaluation, utilisateur, admin);
        String key = UUID.randomUUID().toString();
        evaluation.getPhotos().add(key);
        evaluationRepository.save(evaluation);
        return s3Repository.getUploadUrl(BUCKET, key);
    }

    // Renvoie les liens pour afficher les photos d'une évaluation
    public List<String> getPhotoDownloadUrls(Long id) {
        EvaluationEntity evaluation = getEvaluationById(id);
        return evaluation.getPhotos().stream()
                .map(key -> s3Repository.getDownloadUrl(BUCKET, key))
                .toList();
    }

    // Seul l'auteur de l'évaluation ou un administrateur peut la modifier
    private void verifierAuteurOuAdmin(EvaluationEntity evaluation, String utilisateur, boolean admin) {
        if (!admin && !evaluation.getEvaluateur().equals(utilisateur)) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres évaluations");
        }
    }
}
