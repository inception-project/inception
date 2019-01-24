package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import java.io.IOException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public interface DocumentImporter
{
    boolean importDocumentFromDocumentRepository(User aUser, Project aProject,
        String aDocumentTitle, DocumentRepository aRepository) throws IOException;
}
