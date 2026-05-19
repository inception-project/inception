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
package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.Map;

import org.apache.uima.jcas.cas.TOP;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.workload.api.ProgressMetric;
import de.tudarmstadt.ukp.inception.workload.api.ProgressWeighter;

/**
 * Bridges the workload progress chart to the search index. Currently supports
 * {@link ProgressMetric#TOKENS} and {@link ProgressMetric#SENTENCES}.
 */
public class ProgressWeighterImpl
    implements ProgressWeighter
{
    private final SearchService searchService;

    public ProgressWeighterImpl(SearchService aSearchService)
    {
        searchService = aSearchService;
    }

    @Override
    public Map<Long, Long> getWeights(User aUser, Project aProject, ProgressMetric aMetric)
        throws IOException
    {
        if (aMetric == null) {
            return null;
        }

        Class<? extends TOP> type = switch (aMetric) {
        case TOKENS -> Token.class;
        case SENTENCES -> Sentence.class;
        case DOCUMENTS -> null;
        };

        if (type == null) {
            return null;
        }

        var layer = AnnotationLayer.builder() //
                .forJCasClass(type) //
                .build();
        try {
            return searchService.getAnnotationCountsPerSourceDocument(aUser, aProject, layer);
        }
        catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
