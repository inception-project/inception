package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DocumentImporter
{

    private static final String PLAIN_TEXT = "text";

    public static void importDocumentFromExternalSearch(ExternalSearchService aExternalSearchService,
        DocumentService aDocumentService, String aDocumentTitle, User aUser, Project aProject,
        DocumentRepository aRepository) throws IOException
    {
        String text = aExternalSearchService.getDocumentById(aUser, aRepository, aDocumentTitle)
            .getText();

        if (aDocumentService.existsSourceDocument(aProject, aDocumentTitle)) {
            throw new IOException("Document [" + aDocumentTitle + "] already uploaded! "
                + "Delete the document if you want to upload again");
        }
        else {
            InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

            SourceDocument document = new SourceDocument();
            document.setName(aDocumentTitle);
            document.setProject(aProject);
            document.setFormat(PLAIN_TEXT);

            try (InputStream is = stream) {
                aDocumentService.uploadSourceDocument(is, document);
            }
            catch (IOException | UIMAException e) {
                throw new IOException("Unable to retrieve document " + aDocumentTitle, e);
            }
        }
    }
}
