package fr.michelin.guide.service;

import fr.michelin.guide.entity.EvaluationEntity;
import fr.michelin.guide.entity.RestaurantEntity;
import fr.michelin.guide.exception.NotFoundException;
import fr.michelin.guide.repository.RestaurantRepository;
import fr.michelin.guide.repository.S3Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires de RestaurantService, le repository et le S3 sont simulés avec Mockito
@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @InjectMocks
    private RestaurantService restaurantService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private S3Repository s3Repository;

    // Liste des restaurants renvoyée par le repository
    @Test
    void should_get_a_not_empty_list() {
        when(restaurantRepository.findAll()).thenReturn(Collections.singletonList(RestaurantEntity.builder().build()));

        List<RestaurantEntity> result = restaurantService.getRestaurants();

        assertEquals(1, result.size());
    }

    // Restaurant existant
    @Test
    void should_get_restaurant_by_id() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).nom("Le Petit Bistrot").build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantEntity result = restaurantService.getRestaurantById(1L);

        assertEquals("Le Petit Bistrot", result.getNom());
    }

    // Restaurant inexistant
    @Test
    void should_throw_not_found_when_restaurant_does_not_exist() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> restaurantService.getRestaurantById(99L));
    }

    // Création : le restaurant est enregistré une seule fois, sans évaluation
    @Test
    void should_create_restaurant() {
        // save renvoie l'entité qu'on lui passe
        when(restaurantRepository.save(any(RestaurantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantEntity result = restaurantService.createRestaurant("Le Petit Bistrot", "12 rue des Lilas, 54000 Nancy");

        assertEquals("Le Petit Bistrot", result.getNom());
        assertEquals("12 rue des Lilas, 54000 Nancy", result.getAdresse());
        assertTrue(result.getEvaluations().isEmpty());
        verify(restaurantRepository, times(1)).save(any(RestaurantEntity.class));
    }

    // Mise à jour : nom et adresse modifiés, une lecture et une écriture en base
    @Test
    void should_update_nom_and_adresse() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).nom("Ancien nom").adresse("Ancienne adresse").build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        RestaurantEntity result = restaurantService.updateRestaurant(1L, "Nouveau nom", "Nouvelle adresse");

        assertEquals("Nouveau nom", result.getNom());
        assertEquals("Nouvelle adresse", result.getAdresse());
        verify(restaurantRepository, times(1)).findById(1L);
        verify(restaurantRepository, times(1)).save(restaurant);
    }

    // Mise à jour d'un restaurant inexistant : rien n'est enregistré
    @Test
    void should_not_update_unknown_restaurant() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> restaurantService.updateRestaurant(99L, "Nom", "Adresse"));
        verify(restaurantRepository, never()).save(any(RestaurantEntity.class));
    }

    // Restaurant sans évaluation : moyenne de -1
    @Test
    void should_return_minus_one_when_restaurant_has_no_evaluation() {
        RestaurantEntity restaurant = RestaurantEntity.builder().build();

        assertEquals(-1.0, restaurantService.getMoyenne(restaurant));
    }

    // Restaurant avec plusieurs évaluations : moyenne des notes
    @Test
    void should_return_average_of_notes() {
        RestaurantEntity restaurant = RestaurantEntity.builder()
                .evaluations(List.of(
                        EvaluationEntity.builder().note(3).build(),
                        EvaluationEntity.builder().note(2).build()))
                .build();

        assertEquals(2.5, restaurantService.getMoyenne(restaurant));
    }

    // Une seule évaluation notée 0 : la moyenne vaut 0 et pas -1
    @Test
    void should_return_zero_when_only_note_is_zero() {
        RestaurantEntity restaurant = RestaurantEntity.builder()
                .evaluations(List.of(EvaluationEntity.builder().note(0).build()))
                .build();

        assertEquals(0.0, restaurantService.getMoyenne(restaurant));
    }

    // Lien d'envoi de l'image : une clé est enregistrée sur le restaurant et le lien du S3 est renvoyé
    @Test
    void should_give_image_upload_url() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(s3Repository.getUploadUrl(eq("restaurant"), anyString())).thenReturn("https://s3/lien-envoi");

        String result = restaurantService.getImageUploadUrl(1L);

        assertEquals("https://s3/lien-envoi", result);
        assertNotNull(restaurant.getImage());
        verify(restaurantRepository, times(1)).save(restaurant);
    }

    // Lien d'affichage de l'image d'un restaurant qui en a une
    @Test
    void should_give_image_download_url() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).image("cle-image").build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(s3Repository.getDownloadUrl("restaurant", "cle-image")).thenReturn("https://s3/lien-affichage");

        assertEquals("https://s3/lien-affichage", restaurantService.getImageDownloadUrl(1L));
    }

    // Restaurant sans image : pas de lien d'affichage
    @Test
    void should_throw_not_found_when_restaurant_has_no_image() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(RestaurantEntity.builder().id(1L).build()));

        assertThrows(NotFoundException.class, () -> restaurantService.getImageDownloadUrl(1L));
    }
}
