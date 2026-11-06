package fr.michelin.guide.controller;

import fr.michelin.guide.dto.RestaurantDto;
import fr.michelin.guide.dto.UrlDto;
import fr.michelin.guide.entity.RestaurantEntity;
import fr.michelin.guide.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Routes REST des restaurants
@RestController
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Liste de tous les restaurants, avec leur moyenne
    @GetMapping("/restaurants")
    public List<RestaurantDto> getRestaurants() {
        return restaurantService.getRestaurants().stream()
                .map(this::toDto)
                .toList();
    }

    // Un restaurant par son id, avec sa moyenne
    @GetMapping("/restaurants/{id}")
    public RestaurantDto getRestaurantById(@PathVariable Long id) {
        return toDto(restaurantService.getRestaurantById(id));
    }

    // Création d'un restaurant à partir de son nom et de son adresse (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/restaurants")
    public RestaurantDto createRestaurant(@Valid @RequestBody RestaurantDto restaurantDto) {
        return toDto(restaurantService.createRestaurant(restaurantDto.getNom(), restaurantDto.getAdresse()));
    }

    // Mise à jour du nom et de l'adresse d'un restaurant (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restaurants/{id}")
    public RestaurantDto updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantDto restaurantDto) {
        return toDto(restaurantService.updateRestaurant(id, restaurantDto.getNom(), restaurantDto.getAdresse()));
    }

    // Lien pour envoyer l'image du restaurant sur le S3 (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/restaurants/{id}/image")
    public UrlDto getImageUploadUrl(@PathVariable Long id) {
        return new UrlDto(restaurantService.getImageUploadUrl(id));
    }

    // Lien pour afficher l'image du restaurant
    @GetMapping("/restaurants/{id}/image")
    public UrlDto getImageDownloadUrl(@PathVariable Long id) {
        return new UrlDto(restaurantService.getImageDownloadUrl(id));
    }

    // Conversion en DTO avec la moyenne calculée par le service
    private RestaurantDto toDto(RestaurantEntity restaurant) {
        return RestaurantDto.fromEntity(restaurant, restaurantService.getMoyenne(restaurant));
    }
}
