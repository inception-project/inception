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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.GazeteerEntry;

public interface GazeteerService
{
    /**
     * List gazeteers for the given recommender.
     */
    @SuppressWarnings("javadoc")
    List<Gazeteer> listGazeteers(Recommender aRecommender);

    /**
     * Delete the given gazetter.
     */
    @SuppressWarnings("javadoc")
    void deleteGazeteers(Gazeteer aGazeteer) throws IOException;

    /**
     * Import the gazeteer file for the given gazeteer.
     */
    @SuppressWarnings("javadoc")
    void importGazeteerFile(Gazeteer aGazeteer, InputStream aStream) throws IOException;

    /**
     * Get the gazeteer file for the given gazeteer. If no file has been imported yet for the given
     * gazeteer, the file returned by this method does not exist.
     */
    @SuppressWarnings("javadoc")
    File getGazeteerFile(Gazeteer aSet) throws IOException;

    /**
     * Write the given gazetter to the database.
     */
    @SuppressWarnings("javadoc")
    void createOrUpdateGazeteer(Gazeteer aGazeteer);

    /**
     * Loads the gazeteer.
     */
    @SuppressWarnings("javadoc")
    List<GazeteerEntry> readGazeteerFile(Gazeteer aGaz) throws IOException;

    boolean existsGazeteer(Recommender aRecommender, String aName);
}
