package fr.michelin.guide.service;

import fr.michelin.guide.entity.EvaluationEntity;
import fr.michelin.guide.entity.RestaurantEntity;
import fr.michelin.guide.exception.NotFoundException;
import fr.michelin.guide.repository.RestaurantRepository;
import fr.michelin.guide.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// Logique métier des restaurants (lecture, création, mise à jour, moyenne, image)
@Service
@RequiredArgsConstructor
public class RestaurantService {

    // Bucket S3 qui contient les images des restaurants
    private static final String BUCKET = "restaurant";

    private final RestaurantRepository restaurantRepository;

    private final S3Repository s3Repository;

    // Récupère tous les restaurants
    public List<RestaurantEntity> getRestaurants() {
        return restaurantRepository.findAll();
    }

    // Récupère un restaurant par son id, ou lève une NotFoundException s'il n'existe pas
    public RestaurantEntity getRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Le restaurant " + id + " n'existe pas"));
    }

    // Crée un nouveau restaurant (sans évaluation ni image au départ)
    public RestaurantEntity createRestaurant(String nom, String adresse) {
        RestaurantEntity restaurant = RestaurantEntity.builder()
                .nom(nom)
                .adresse(adresse)
                .build();
        return restaurantRepository.save(restaurant);
    }

    // Met à jour uniquement le nom et l'adresse d'un restaurant existant
    public RestaurantEntity updateRestaurant(Long id, String nom, String adresse) {
        RestaurantEntity restaurant = getRestaurantById(id);
        restaurant.setNom(nom);
        restaurant.setAdresse(adresse);
        return restaurantRepository.save(restaurant);
    }

    // Moyenne des notes du restaurant, ou -1 s'il n'a aucune évaluation
    public double getMoyenne(RestaurantEntity restaurant) {
        return restaurant.getEvaluations().stream()
                .mapToInt(EvaluationEntity::getNote)
                .average()
                .orElse(-1);
    }

    // Génère une nouvelle clé d'image pour le restaurant et renvoie le lien pour envoyer l'image sur le S3
    public String getImageUploadUrl(Long id) {
        RestaurantEntity restaurant = getRestaurantById(id);
        String key = UUID.randomUUID().toString();
        restaurant.setImage(key);
        restaurantRepository.save(restaurant);
        return s3Repository.getUploadUrl(BUCKET, key);
    }

    // Renvoie le lien pour afficher l'image du restaurant
    public String getImageDownloadUrl(Long id) {
        RestaurantEntity restaurant = getRestaurantById(id);
        if (restaurant.getImage() == null) {
            throw new NotFoundException("Le restaurant " + id + " n'a pas d'image");
        }
        return s3Repository.getDownloadUrl(BUCKET, restaurant.getImage());
    }
}
