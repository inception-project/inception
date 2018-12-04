package de.tudarmstadt.ukp.inception.search;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.search.model.Index;

import java.io.IOException;

public interface ProjectIndexingService {
    void reindex(Project aproject) throws IOException;
    Index getIndex(Project aProject);
    boolean isIndexValid(Project aProject);
}
