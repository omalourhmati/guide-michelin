package fr.michelin.guide.repository;

import fr.michelin.guide.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Accès à la table "restaurant" (méthodes de base fournies par JpaRepository)
@Repository
public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
}
