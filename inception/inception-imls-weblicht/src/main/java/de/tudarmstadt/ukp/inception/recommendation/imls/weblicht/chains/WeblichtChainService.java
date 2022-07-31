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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;

public interface WeblichtChainService
{
    /**
     * @return the chain for the given.
     */
    @SuppressWarnings("javadoc")
    Optional<WeblichtChain> getChain(Recommender aRecommender);

    /**
     * Delete the chain for the given recommender.
     */
    @SuppressWarnings("javadoc")
    void deleteChain(WeblichtChain aChain) throws IOException;

    /**
     * Import the chain file for the given chain.
     */
    @SuppressWarnings("javadoc")
    void importChainFile(WeblichtChain aChain, InputStream aStream) throws IOException;

    /**
     * @return the chain file for the given chain. If no file has been imported yet for the given
     *         chain, the file returned by this method does not exist.
     */
    @SuppressWarnings("javadoc")
    File getChainFile(WeblichtChain aSet) throws IOException;

    /**
     * Write the given chain to the database.
     */
    @SuppressWarnings("javadoc")
    void createOrUpdateChain(WeblichtChain aChain);

    boolean existsChain(Recommender aRecommender);
}
