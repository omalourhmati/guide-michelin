package fr.michelin.guide.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests de IndexService avec un vrai index Lucene créé dans un dossier temporaire
public class IndexServiceTest {

    @TempDir
    private Path dossierTemporaire;

    private IndexService indexService;

    @BeforeEach
    void creerIndex() throws IOException {
        indexService = new IndexService(dossierTemporaire.toString());
        indexService.indexEvaluation(1L, "Poisson très frais et dessert maison");
        indexService.indexEvaluation(2L, "Viande trop cuite");
        indexService.indexEvaluation(3L, "Excellent dessert au chocolat");
    }

    @AfterEach
    void fermerIndex() throws IOException {
        indexService.fermer();
    }

    // Un mot-clé présent dans un seul commentaire
    @Test
    void should_find_evaluation_containing_keyword() {
        assertEquals(List.of(2L), indexService.searchEvaluations("viande"));
    }

    // Plusieurs mots-clés : une évaluation est trouvée si elle contient au moins un des mots
    @Test
    void should_find_evaluations_containing_one_of_the_keywords() {
        List<Long> result = indexService.searchEvaluations("poisson chocolat");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(1L, 3L)));
    }

    // La recherche ne tient pas compte des majuscules
    @Test
    void should_ignore_case() {
        assertEquals(2, indexService.searchEvaluations("DESSERT").size());
    }

    // Aucun commentaire ne contient le mot
    @Test
    void should_return_empty_list_when_nothing_matches() {
        assertTrue(indexService.searchEvaluations("pizza").isEmpty());
    }

    // Mots-clés vides : aucun résultat
    @Test
    void should_return_empty_list_for_blank_keywords() {
        assertTrue(indexService.searchEvaluations("   ").isEmpty());
    }

    // Une évaluation retirée de l'index n'est plus trouvée
    @Test
    void should_not_find_removed_evaluation() {
        indexService.removeEvaluation(2L);

        assertTrue(indexService.searchEvaluations("viande").isEmpty());
    }

    // Réindexer une évaluation remplace son ancien commentaire
    @Test
    void should_replace_comment_when_indexed_again() {
        indexService.indexEvaluation(2L, "Viande parfaite");

        assertEquals(List.of(2L), indexService.searchEvaluations("parfaite"));
        assertTrue(indexService.searchEvaluations("cuite").isEmpty());
    }

    // Les caractères spéciaux de Lucene ne provoquent pas d'erreur
    @Test
    void should_accept_special_characters() {
        assertTrue(indexService.searchEvaluations("dessert?(").contains(1L));
    }
}
