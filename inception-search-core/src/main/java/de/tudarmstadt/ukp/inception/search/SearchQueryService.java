package de.tudarmstadt.ukp.inception.search;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

import java.io.IOException;
import java.util.List;

public interface SearchQueryService {
    List<SearchResult> query(User aUser, Project aProject, String aQuery)
            throws IOException, ExecutionException;
}
