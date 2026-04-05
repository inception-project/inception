/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.Gazetteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.GazetteerEntry;

public interface GazetteerService
{
    /**
     * List gazetteers for the given recommender.
     */
    @SuppressWarnings("javadoc")
    List<Gazetteer> listGazetteers(Recommender aRecommender);

    /**
     * Delete the given gazetteer.
     */
    @SuppressWarnings("javadoc")
    void deleteGazetteers(Gazetteer aGazetteer) throws IOException;

    /**
     * Import the gazetteer file for the given gazetteer.
     */
    @SuppressWarnings("javadoc")
    void importGazetteerFile(Gazetteer aGazetteer, InputStream aStream) throws IOException;

    /**
     * Get the gazetteer file for the given gazetteer. If no file has been imported yet for the
     * given gazetteer, the file returned by this method does not exist.
     */
    @SuppressWarnings("javadoc")
    File getGazetteerFile(Gazetteer aSet) throws IOException;

    /**
     * Write the given gazetteer to the database.
     */
    @SuppressWarnings("javadoc")
    void createOrUpdateGazetteer(Gazetteer aGazetteer);

    /**
     * Loads the gazetteer.
     */
    @SuppressWarnings("javadoc")
    List<GazetteerEntry> readGazetteerFile(Gazetteer aGaz) throws IOException;

    /**
     * Parse the gazetteer input stream into the provided target list. This can be
     * used for validation before persisting the gazetteer.
     */
    @SuppressWarnings("javadoc")
    void parseGazetteer(Gazetteer aGaz, InputStream aStream, List<GazetteerEntry> aTarget)
        throws IOException;

    boolean existsGazetteer(Recommender aRecommender, String aName);
}
