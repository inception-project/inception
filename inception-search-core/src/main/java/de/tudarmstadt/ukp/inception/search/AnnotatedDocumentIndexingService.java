package de.tudarmstadt.ukp.inception.search;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import org.apache.uima.jcas.JCas;

public interface AnnotatedDocumentIndexingService {
    void indexDocument(AnnotationDocument aAnnotationDocument, JCas aJCas);
}
