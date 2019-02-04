package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;

public interface GazeteerService
{
    /**
     * List gazeteers for the given recommender.
     */
    List<Gazeteer> listGazeteers(Recommender aRecommender);
    
    /**
     * Delete the given gazetter.
     */
    void deleteGazeteers(Gazeteer aGazeteer) throws IOException;
    
    /**
     * Import the gazeteer file for the given gazeteer.
     */
    void importGazeteerFile(Gazeteer aGazeteer, InputStream aStream) throws IOException;
    
    /**
     * Get the gazeteer file for the given gazeteer. If no file has been imported yet for the given
     * gazeteer, the file returned by this method does not exist.
     */
    File getGazeteerFile(Gazeteer aSet) throws IOException;

    /**
     * Write the given gazetter to the database.
     */
    void createOrUpdateGazeteer(Gazeteer aGazeteer);

    /**
     * Loads the given gazetter into a map.
     */
    Map<String, String> readGazeteerFile(Gazeteer aGaz) throws IOException;
}
