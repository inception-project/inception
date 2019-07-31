package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SearchResultsProvider implements IDataProvider
{
    private @SpringBean SearchService searchService;

    private User user;
    private Project project;
    private String query;
    private SourceDocument document;
    private AnnotationLayer annotationLayer;
    private AnnotationFeature annotationFeature;

    public SearchResultsProvider(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature) {
        user = aUser;
        project = aProject;
        query = aQuery;
        document = aDocument;
        annotationLayer = aAnnotationLayer;
        annotationFeature = aAnnotationFeature;

    }

    public Iterator iterator(long first, long count)
    {
        try {
            Map<String, ResultsGroup> queryResults = searchService
                .query(user, project, query, document,
                    annotationLayer, annotationFeature, first, count)
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e ->
                    new ResultsGroup(e.getKey(), e.getValue())));
            return queryResults.entrySet().iterator();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long size()
    {
        return 10L;
    }

    public IModel model(Object object)
    {
        return new Model((SearchResult)object);
    }
}

