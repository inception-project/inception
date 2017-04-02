package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.File;
import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public interface CasStorageService
{
    /**
     * Creates an annotation document (either user's annotation document or CURATION_USER's
     * annotation document)
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aJcas
     *            The annotated CAS object
     * @param aUserName
     *            the user who annotates the document if it is user's annotation document OR the
     *            CURATION_USER
     */
    void writeCas(SourceDocument aDocument, JCas aJcas, String aUserName)
        throws IOException;

    /**
     * For a given {@link SourceDocument}, return the {@link AnnotationDocument} for the user or for
     * the CURATION_USER
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aUsername
     *            the {@link User} who annotates the {@link SourceDocument} or the CURATION_USER
     */
    JCas readCas(SourceDocument aDocument, String aUsername)
        throws IOException;
    
    File getAnnotationFolder(SourceDocument aDocument)
            throws IOException;

    void analyzeAndRepair(SourceDocument aDocument, String aUsername, CAS aCas);
}
