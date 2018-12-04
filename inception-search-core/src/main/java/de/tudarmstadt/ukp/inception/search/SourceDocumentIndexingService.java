package de.tudarmstadt.ukp.inception.search;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import org.apache.uima.jcas.JCas;

public interface SourceDocumentIndexingService {
    void indexDocument(SourceDocument aSourceDocument, JCas aJCas);
}
