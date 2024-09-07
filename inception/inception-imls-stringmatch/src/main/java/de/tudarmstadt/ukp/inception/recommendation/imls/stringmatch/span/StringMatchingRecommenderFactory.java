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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import org.apache.uima.cas.CAS;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.settings.StringMatchingRecommenderTraitsEditor;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#stringMatchingRecommenderFactory}.
 * </p>
 */
public class StringMatchingRecommenderFactory
    extends RecommendationEngineFactoryImplBase<StringMatchingRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender";

    private final GazeteerService gazeteerService;

    public StringMatchingRecommenderFactory(GazeteerService aGazeteerService)
    {
        gazeteerService = aGazeteerService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        var traits = readTraits(aRecommender);
        return new StringMatchingRecommender(aRecommender, traits, gazeteerService);
    }

    @Override
    public String getName()
    {
        return "String Matcher";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }

        // We exclude sentence level for the moment for no better reason than that would probably
        // generate quite large dictionaries...
        return (asList(CHARACTERS, SINGLE_TOKEN, TOKENS).contains(aLayer.getAnchoringMode()))
                && SPAN_TYPE.equals(aLayer.getType())
                && (TYPE_NAME_STRING_ARRAY.equals(aFeature.getType())
                        // not all are supported/tested yet ||
                        // ICasUtil.isPrimitive(aFeature.getType())
                        || asList(CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_BOOLEAN)
                                .contains(aFeature.getType())
                        || aFeature.isVirtualFeature());
    }

    @Override
    public StringMatchingRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new StringMatchingRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public StringMatchingRecommenderTraits createTraits()
    {
        return new StringMatchingRecommenderTraits();
    }

    @Override
    public boolean isModelExportSupported()
    {
        return true;
    }

    @Override
    public String getExportModelName(Recommender aRecommender)
    {
        return "model.txt";
    }
}
