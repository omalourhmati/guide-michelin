package fr.michelin.guide.controller;

import fr.michelin.guide.dto.EvaluationDto;
import fr.michelin.guide.dto.UrlDto;
import fr.michelin.guide.entity.EvaluationEntity;
import fr.michelin.guide.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Routes REST des évaluations
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // Ajout d'une évaluation sur le restaurant {id}, l'évaluateur est l'utilisateur connecté
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/restaurants/{id}/evaluations")
    public EvaluationDto createEvaluation(@PathVariable Long id, @Valid @RequestBody EvaluationDto evaluationDto,
                                          Authentication authentication) {
        EvaluationEntity evaluation = evaluationService.createEvaluation(id, getNomUtilisateur(authentication),
                evaluationDto.getCommentaire(), evaluationDto.getNote());
        return EvaluationDto.fromEntity(evaluation);
    }

    // Recherche des évaluations par un ou plusieurs mots-clés (ex : ?motsCles=poisson&motsCles=dessert)
    @GetMapping("/evaluations")
    public List<EvaluationDto> searchEvaluations(@RequestParam List<String> motsCles) {
        return evaluationService.searchEvaluations(motsCles).stream()
                .map(EvaluationDto::fromEntity)
                .toList();
    }

    // Évaluations écrites par l'utilisateur connecté
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/evaluations")
    public List<EvaluationDto> getMesEvaluations(Authentication authentication) {
        return evaluationService.getEvaluationsByEvaluateur(getNomUtilisateur(authentication)).stream()
                .map(EvaluationDto::fromEntity)
                .toList();
    }

    // Modification du commentaire et de la note par l'auteur de l'évaluation ou par un administrateur
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/evaluations/{id}")
    public EvaluationDto updateEvaluation(@PathVariable Long id, @Valid @RequestBody EvaluationDto evaluationDto,
                                          Authentication authentication) {
        EvaluationEntity evaluation = evaluationService.updateEvaluation(id, evaluationDto.getCommentaire(),
                evaluationDto.getNote(), getNomUtilisateur(authentication), isAdmin(authentication));
        return EvaluationDto.fromEntity(evaluation);
    }

    // Suppression d'une évaluation par son auteur ou par un administrateur
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/evaluations/{id}")
    public void deleteEvaluation(@PathVariable Long id, Authentication authentication) {
        evaluationService.deleteEvaluation(id, getNomUtilisateur(authentication), isAdmin(authentication));
    }

    // Lien pour envoyer une photo de plat sur le S3 (auteur de l'évaluation ou ADMIN)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/evaluations/{id}/photos")
    public UrlDto getPhotoUploadUrl(@PathVariable Long id, Authentication authentication) {
        return new UrlDto(evaluationService.getPhotoUploadUrl(id, getNomUtilisateur(authentication), isAdmin(authentication)));
    }

    // Liens pour afficher les photos d'une évaluation
    @GetMapping("/evaluations/{id}/photos")
    public List<String> getPhotoDownloadUrls(@PathVariable Long id) {
        return evaluationService.getPhotoDownloadUrls(id);
    }

    // Nom de l'utilisateur connecté, lu dans le token
    private String getNomUtilisateur(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("name");
    }

    // Vrai si l'utilisateur connecté a le rôle ADMIN
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
