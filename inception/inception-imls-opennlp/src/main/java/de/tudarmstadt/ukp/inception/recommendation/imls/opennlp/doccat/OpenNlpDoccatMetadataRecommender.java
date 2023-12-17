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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import static org.apache.uima.fit.util.CasUtil.getType;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import opennlp.tools.doccat.DocumentSample;

public class OpenNlpDoccatMetadataRecommender
    extends OpenNlpDoccatRecommender
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NO_CATEGORY = "<NO_CATEGORY>";

    public OpenNlpDoccatMetadataRecommender(Recommender aRecommender,
            OpenNlpDoccatRecommenderTraits aTraits)
    {
        super(aRecommender, aTraits);
    }

    @Override
    protected Class<DocumentAnnotation> getSampleUnit()
    {
        return DocumentAnnotation.class;
    }

    @Override
    protected Class<DocumentAnnotation> getDataPointUnit()
    {
        return DocumentAnnotation.class;
    }

    @Override
    protected List<DocumentSample> extractSamples(List<CAS> aCasses)
    {
        var samples = new ArrayList<DocumentSample>();

        for (var cas : aCasses) {
            var tokenTexts = cas.select(Token.class).map(AnnotationFS::getCoveredText)
                    .toArray(String[]::new);

            var annotationType = getType(cas, layerName);
            var annotation = cas.select(annotationType).nullOK().get();
            if (annotation == null) {
                continue;
            }

            var feature = annotationType.getFeatureByBaseName(featureName);
            var label = annotation.getFeatureValueAsString(feature);
            var nameSample = new DocumentSample(label != null ? label : NO_CATEGORY, tokenTexts);
            if (nameSample.getCategory() != null) {
                samples.add(nameSample);
            }
        }

        return samples;
    }
}
