# API Guide Michelin

API REST réalisée avec Spring Boot pour gérer les restaurants visités par le guide Michelin et leurs évaluations.

## Technologies

- Java 21, Spring Boot 4.1.2, Maven
- Spring Data JPA avec une base H2 enregistrée dans un fichier (`cours.mv.db`)
- Spring Security en OAuth2 Resource Server : authentification par token JWT délivré par Keycloak
- AWS SDK S3 : images stockées sur le S3 avec des liens présignés
- Apache Lucene 9.11.1 : recherche des évaluations par mots-clés
- springdoc-openapi : documentation Swagger
- JUnit 5 et Mockito : tests unitaires des services

## Lancer le projet

Prérequis : Java 21 et Maven.

```bash
mvn spring-boot:run
```

L'API démarre sur `http://localhost:8080`. La base H2 et l'index Lucene sont créés automatiquement au premier lancement.

Autres commandes :

```bash
mvn test                                    # lance les tests unitaires
mvn package                                 # construit le jar
java -jar target/guide-0.0.1-SNAPSHOT.jar   # lance le jar
```

## Documentation Swagger

- Interface : http://localhost:8080/swagger-ui.html
- Description OpenAPI : http://localhost:8080/v3/api-docs.yaml

## Authentification

- Keycloak : `https://keycloak.bober.ovh/realms/master`, client `coursm2`
- Les rôles sont lus dans le token (`resource_access.coursm2.roles`) et deviennent `ROLE_USER` ou `ROLE_ADMIN` dans Spring.
- Le nom de l'évaluateur est pris dans le token (claim `name`), il n'est jamais envoyé par le client.

Utilisateurs de test (mots de passe fournis dans l'énoncé) :

| Utilisateur                  | Rôle  |
|------------------------------|-------|
| lucien.bramard@michelin.fr   | USER  |
| noel.flantier@michelin.fr    | ADMIN |

Obtenir un token :

```bash
curl -X POST https://keycloak.bober.ovh/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=coursm2 \
  -d username=lucien.bramard@michelin.fr -d password=<mot de passe>
```

Le token est valable 60 secondes. Il s'envoie dans l'en-tête `Authorization: Bearer <token>`.

## Routes

| Méthode | Route                                   | Accès                 | Description                                           |
|---------|-----------------------------------------|-----------------------|-------------------------------------------------------|
| GET     | `/restaurants`                          | tout le monde         | Liste des restaurants avec leur moyenne              |
| GET     | `/restaurants/{id}`                     | tout le monde         | Un restaurant avec sa moyenne                         |
| POST    | `/restaurants`                          | ADMIN                 | Création d'un restaurant (nom, adresse)               |
| PUT     | `/restaurants/{id}`                     | ADMIN                 | Modification du nom et de l'adresse                   |
| POST    | `/restaurants/{id}/image`               | ADMIN                 | Lien pour envoyer l'image du restaurant               |
| GET     | `/restaurants/{id}/image`               | tout le monde         | Lien pour afficher l'image du restaurant              |
| POST    | `/restaurants/{id}/evaluations`         | USER, ADMIN           | Ajout d'une évaluation (commentaire, note)            |
| PUT     | `/evaluations/{id}`                     | auteur ou ADMIN       | Modification du commentaire et de la note             |
| DELETE  | `/evaluations/{id}`                     | auteur ou ADMIN       | Suppression d'une évaluation                          |
| POST    | `/evaluations/{id}/photos`              | auteur ou ADMIN       | Lien pour envoyer une photo de plat                   |
| GET     | `/evaluations/{id}/photos`              | tout le monde         | Liens pour afficher les photos d'une évaluation       |
| GET     | `/evaluations?motsCles=a&motsCles=b`    | tout le monde         | Recherche des évaluations par un ou plusieurs mots    |
| GET     | `/user/evaluations`                     | utilisateur connecté  | Évaluations écrites par l'utilisateur connecté        |

La propriété `moyenne` contient la moyenne des notes du restaurant, ou `-1` s'il n'a aucune évaluation.

## Erreurs

Toutes les erreurs ont le même format :

```json
{ "code": 404, "message": "Le restaurant 99 n'existe pas" }
```

- 400 : données invalides (nom vide, note en dehors de 0 à 3, ...)
- 401 : utilisateur non connecté, ou token invalide ou expiré
- 403 : droits insuffisants (rôle ou évaluation d'un autre utilisateur)
- 404 : restaurant, évaluation ou image introuvable
- 500 : erreur interne (le détail est seulement écrit dans les logs)

## Images (S3)

Les fichiers ne passent pas par l'API. L'API génère une clé, l'enregistre en base et renvoie un lien présigné valable 10 minutes. Le client envoie ensuite l'image directement sur le S3 avec ce lien :

```bash
curl -X PUT -H "Content-Type: image/jpeg" --upload-file image.jpg "<lien>"
```

Buckets utilisés : `restaurant` (image du restaurant) et `evaluation` (photos des plats).

## Recherche par mots-clés

Les commentaires des évaluations sont indexés avec Lucene (dossier `index`). L'index est reconstruit au démarrage à partir de la base, puis mis à jour à chaque ajout, modification ou suppression d'évaluation. Une évaluation est trouvée si son commentaire contient au moins un des mots-clés, sans tenir compte des majuscules.

## Organisation du code

```
fr.michelin.guide
├── config       sécurité (Keycloak, rôles) et description Swagger
├── controller   routes REST et gestion des erreurs
├── dto          objets échangés en JSON
├── entity       tables de la base
├── exception    exceptions de l'application (404, 403)
├── repository   accès à la base et au S3
└── service      logique métier et index Lucene
```

## Tests

Les services sont testés avec JUnit 5 et Mockito (repositories et S3 simulés) : restaurants avec et sans évaluations, calcul de la moyenne, droits sur les évaluations, liens S3, recherche. `IndexService` est testé avec un vrai index Lucene créé dans un dossier temporaire.
