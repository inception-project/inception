/*
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
package de.tudarmstadt.ukp.inception.support.test.dkpro;

import java.io.File;

import org.dkpro.core.api.datasets.DatasetFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;

/**
 * This is not really a test, it's just to make sure the datasets have been downloaded and
 * materialized because the DKPro Core Datasets API is currently not thread-safe when it comes to
 * materialization but we do want to be able to run parallel Maven builds.
 */
class DatasetPreloadingTest
{
    private static final File cache = DkproTestHelper.getCacheFolder();
    private static final DatasetFactory loader = new DatasetFactory(cache);

    @Test
    void loadDatasets() throws Exception
    {
        loader.load("germeval2014-de");
        loader.load("sentence-classification-en");
        loader.load("gum-en-conll-3.0.0");
    }
}
