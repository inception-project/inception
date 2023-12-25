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
package de.tudarmstadt.ukp.inception.recommendation.service;

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.recommendation.relation.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.span.SpanSuggestionSupport;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar.MetadataSuggestionSupport;

public class SuggestionExtraction
{
    public static List<AnnotationSuggestion> extractSuggestions(int aGeneration, CAS aOriginalCas,
            CAS aPredictionCas, SourceDocument aDocument, Recommender aRecommender)
    {
        var ctx = new ExtractionContext(aGeneration, aRecommender, aDocument, aOriginalCas,
                aPredictionCas);

        switch (ctx.getLayer().getType()) {
        case SpanLayerSupport.TYPE: {
            return SpanSuggestionSupport.extractSuggestions(ctx);
        }
        case RelationLayerSupport.TYPE: {
            return RelationSuggestionSupport.extractSuggestions(ctx);
        }
        case DocumentMetadataLayerSupport.TYPE: {
            return MetadataSuggestionSupport.extractSuggestions(ctx);
        }
        default:
            throw new IllegalStateException(
                    "Unsupported layer type [" + ctx.getLayer().getType() + "]");
        }
    }
}
