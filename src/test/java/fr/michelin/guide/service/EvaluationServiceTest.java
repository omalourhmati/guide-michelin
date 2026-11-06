package fr.michelin.guide.service;

import fr.michelin.guide.entity.EvaluationEntity;
import fr.michelin.guide.entity.RestaurantEntity;
import fr.michelin.guide.exception.ForbiddenException;
import fr.michelin.guide.exception.NotFoundException;
import fr.michelin.guide.repository.EvaluationRepository;
import fr.michelin.guide.repository.S3Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires de EvaluationService, le repository, RestaurantService, le S3 et l'index sont simulés
@ExtendWith(MockitoExtension.class)
public class EvaluationServiceTest {

    @InjectMocks
    private EvaluationService evaluationService;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private S3Repository s3Repository;

    @Mock
    private IndexService indexService;

    // Ajout d'une évaluation sur un restaurant existant
    @Test
    void should_create_evaluation_on_existing_restaurant() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).build();
        when(restaurantService.getRestaurantById(1L)).thenReturn(restaurant);
        // save renvoie l'entité qu'on lui passe
        when(evaluationRepository.save(any(EvaluationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationEntity result = evaluationService.createEvaluation(1L, "Lucien Bramard", "Très bon repas", 3);

        assertEquals("Lucien Bramard", result.getEvaluateur());
        assertEquals("Très bon repas", result.getCommentaire());
        assertEquals(3, result.getNote());
        assertEquals(restaurant, result.getRestaurant());
        verify(evaluationRepository, times(1)).save(any(EvaluationEntity.class));
        verify(indexService, times(1)).indexEvaluation(result.getId(), "Très bon repas");
    }

    // Ajout sur un restaurant inexistant : rien n'est enregistré
    @Test
    void should_not_create_evaluation_when_restaurant_does_not_exist() {
        when(restaurantService.getRestaurantById(99L)).thenThrow(new NotFoundException("Le restaurant 99 n'existe pas"));

        assertThrows(NotFoundException.class,
                () -> evaluationService.createEvaluation(99L, "Lucien Bramard", "Très bon repas", 3));
        verify(evaluationRepository, never()).save(any(EvaluationEntity.class));
    }

    // Évaluations d'un évaluateur
    @Test
    void should_get_evaluations_of_evaluateur() {
        when(evaluationRepository.findByEvaluateur("Lucien Bramard")).thenReturn(List.of(
                EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build(),
                EvaluationEntity.builder().id(2L).evaluateur("Lucien Bramard").build()));

        List<EvaluationEntity> result = evaluationService.getEvaluationsByEvaluateur("Lucien Bramard");

        assertEquals(2, result.size());
    }

    // L'auteur peut supprimer sa propre évaluation
    @Test
    void should_delete_own_evaluation() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1L, "Lucien Bramard", false);

        verify(evaluationRepository, times(1)).delete(evaluation);
        verify(indexService, times(1)).removeEvaluation(1L);
    }

    // Un administrateur peut supprimer l'évaluation de quelqu'un d'autre
    @Test
    void should_let_admin_delete_any_evaluation() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1L, "Noël Flantier", true);

        verify(evaluationRepository, times(1)).delete(evaluation);
    }

    // Un utilisateur ne peut pas supprimer l'évaluation d'un autre : rien n'est supprimé
    @Test
    void should_not_delete_evaluation_of_another_user() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThrows(ForbiddenException.class, () -> evaluationService.deleteEvaluation(1L, "Autre Personne", false));
        verify(evaluationRepository, never()).delete(any(EvaluationEntity.class));
        verify(indexService, never()).removeEvaluation(any());
    }

    // Suppression d'une évaluation inexistante : rien n'est supprimé
    @Test
    void should_throw_not_found_when_deleting_unknown_evaluation() {
        when(evaluationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> evaluationService.deleteEvaluation(99L, "Lucien Bramard", false));
        verify(evaluationRepository, never()).delete(any(EvaluationEntity.class));
    }

    // L'auteur ajoute une photo : la clé est enregistrée et le lien d'envoi du S3 est renvoyé
    @Test
    void should_give_photo_upload_url_to_author() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(s3Repository.getUploadUrl(eq("evaluation"), anyString())).thenReturn("https://s3/lien-envoi");

        String result = evaluationService.getPhotoUploadUrl(1L, "Lucien Bramard", false);

        assertEquals("https://s3/lien-envoi", result);
        assertEquals(1, evaluation.getPhotos().size());
        verify(evaluationRepository, times(1)).save(evaluation);
    }

    // Un autre utilisateur ne peut pas ajouter de photo : rien n'est enregistré
    @Test
    void should_not_give_photo_upload_url_to_another_user() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard").build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThrows(ForbiddenException.class, () -> evaluationService.getPhotoUploadUrl(1L, "Autre Personne", false));
        verify(evaluationRepository, never()).save(any(EvaluationEntity.class));
    }

    // Un lien d'affichage par photo de l'évaluation
    @Test
    void should_give_one_download_url_per_photo() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L)
                .photos(List.of("photo-1", "photo-2"))
                .build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(s3Repository.getDownloadUrl(eq("evaluation"), anyString())).thenReturn("https://s3/lien-affichage");

        List<String> result = evaluationService.getPhotoDownloadUrls(1L);

        assertEquals(2, result.size());
    }

    // Recherche : les ids trouvés par l'index sont chargés depuis la base
    @Test
    void should_search_evaluations_by_keywords() {
        when(indexService.searchEvaluations("poisson dessert")).thenReturn(List.of(1L, 2L));
        when(evaluationRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                EvaluationEntity.builder().id(1L).build(),
                EvaluationEntity.builder().id(2L).build()));

        List<EvaluationEntity> result = evaluationService.searchEvaluations(List.of("poisson", "dessert"));

        assertEquals(2, result.size());
    }

    // L'auteur modifie son évaluation : commentaire et note changent, l'index est mis à jour
    @Test
    void should_update_own_evaluation() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard")
                .commentaire("Ancien commentaire").note(1).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.save(evaluation)).thenReturn(evaluation);

        EvaluationEntity result = evaluationService.updateEvaluation(1L, "Nouveau commentaire", 3, "Lucien Bramard", false);

        assertEquals("Nouveau commentaire", result.getCommentaire());
        assertEquals(3, result.getNote());
        verify(evaluationRepository, times(1)).save(evaluation);
        verify(indexService, times(1)).indexEvaluation(1L, "Nouveau commentaire");
    }

    // Un administrateur peut modifier l'évaluation de quelqu'un d'autre
    @Test
    void should_let_admin_update_any_evaluation() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard")
                .commentaire("Ancien commentaire").note(1).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.save(evaluation)).thenReturn(evaluation);

        EvaluationEntity result = evaluationService.updateEvaluation(1L, "Corrigé par l'admin", 2, "Noël Flantier", true);

        assertEquals("Corrigé par l'admin", result.getCommentaire());
        assertEquals("Lucien Bramard", result.getEvaluateur());
    }

    // Un autre utilisateur ne peut pas modifier l'évaluation : rien n'est enregistré
    @Test
    void should_not_update_evaluation_of_another_user() {
        EvaluationEntity evaluation = EvaluationEntity.builder().id(1L).evaluateur("Lucien Bramard")
                .commentaire("Ancien commentaire").note(1).build();
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThrows(ForbiddenException.class,
                () -> evaluationService.updateEvaluation(1L, "Piraté", 0, "Autre Personne", false));
        assertEquals("Ancien commentaire", evaluation.getCommentaire());
        verify(evaluationRepository, never()).save(any(EvaluationEntity.class));
    }
}
