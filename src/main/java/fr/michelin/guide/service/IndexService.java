package fr.michelin.guide.service;

import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Index Lucene des commentaires d'évaluation, utilisé pour la recherche par mots-clés
@Service
public class IndexService {

    // Champs d'un document de l'index : l'id de l'évaluation et son commentaire
    private static final String CHAMP_ID = "id";
    private static final String CHAMP_CONTENU = "content";

    // Nombre maximum de résultats renvoyés par une recherche
    private static final int MAX_RESULTATS = 100;

    private final IndexWriter indexWriter;
    private final QueryParser queryParser;
    private DirectoryReader directoryReader;
    private IndexSearcher indexSearcher;

    // L'index est recréé vide à chaque démarrage, puis rempli à partir de la base
    public IndexService(@Value("${lucene.dossier}") String dossier) throws IOException {
        Directory index = FSDirectory.open(Paths.get(dossier));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        this.queryParser = new QueryParser(CHAMP_CONTENU, analyzer);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        this.indexWriter = new IndexWriter(index, indexWriterConfig);
        this.indexWriter.commit();
        this.directoryReader = DirectoryReader.open(indexWriter);
        this.indexSearcher = new IndexSearcher(directoryReader);
    }

    // Ajoute le commentaire d'une évaluation dans l'index (ou le remplace s'il y est déjà)
    public synchronized void indexEvaluation(Long id, String commentaire) {
        try {
            Document document = new Document();
            document.add(new StringField(CHAMP_ID, id.toString(), Field.Store.YES));
            document.add(new TextField(CHAMP_CONTENU, commentaire, Field.Store.YES));
            indexWriter.updateDocument(new Term(CHAMP_ID, id.toString()), document);
            indexWriter.commit();
            rafraichirLecture();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Retire une évaluation de l'index
    public synchronized void removeEvaluation(Long id) {
        try {
            indexWriter.deleteDocuments(new Term(CHAMP_ID, id.toString()));
            indexWriter.commit();
            rafraichirLecture();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Ids des évaluations dont le commentaire contient au moins un des mots-clés
    public synchronized List<Long> searchEvaluations(String motsCles) {
        List<Long> ids = new ArrayList<>();
        if (motsCles == null || motsCles.isBlank()) {
            return ids;
        }
        try {
            // escape : les caractères spéciaux de Lucene (*, ?, :, ...) sont cherchés comme du texte normal
            Query query = queryParser.parse(QueryParser.escape(motsCles));
            TopDocs topDocs = indexSearcher.search(query, MAX_RESULTATS);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                String id = indexSearcher.storedFields().document(scoreDoc.doc).get(CHAMP_ID);
                ids.add(Long.valueOf(id));
            }
            return ids;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Après une écriture, on rouvre le lecteur pour que les recherches voient les changements
    private void rafraichirLecture() throws IOException {
        DirectoryReader nouveauReader = DirectoryReader.openIfChanged(directoryReader, indexWriter);
        if (nouveauReader != null) {
            directoryReader.close();
            directoryReader = nouveauReader;
            indexSearcher = new IndexSearcher(directoryReader);
        }
    }

    // Fermeture propre de l'index à l'arrêt de l'application
    @PreDestroy
    public synchronized void fermer() throws IOException {
        directoryReader.close();
        indexWriter.close();
    }
}
