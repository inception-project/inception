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
package de.tudarmstadt.ukp.inception.support.test.recommendation;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;

public class RecommenderTestHelper
{

    public static void addScoreFeature(CAS aCas, String aTypeName, String aFeatureName)
        throws IOException, UIMAException
    {
        String scoreFeatureName = aFeatureName + FEATURE_NAME_SCORE_SUFFIX;
        String scoreExplanationFeatureName = aFeatureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;

        TypeSystemDescription tsd = typeSystem2TypeSystemDescription(aCas.getTypeSystem());
        TypeDescription typeDescription = tsd.getType(aTypeName);
        typeDescription.addFeature(scoreFeatureName, "Score feature", TYPE_NAME_DOUBLE);
        typeDescription.addFeature(scoreExplanationFeatureName, "Score explanation feature",
                TYPE_NAME_STRING);
        typeDescription.addFeature(FEATURE_NAME_IS_PREDICTION, "Is prediction", TYPE_NAME_BOOLEAN);

        AnnotationSchemaService schemaService = new AnnotationSchemaServiceImpl();
        schemaService.upgradeCas(aCas, tsd);
    }

    public static <T extends Annotation> void addScoreFeature(CAS aCas, Class<T> aClass,
            String aFeatureName)
        throws IOException, UIMAException
    {
        addScoreFeature(aCas, aClass.getName(), aFeatureName);
    }

    public static double getScore(AnnotationFS aAnnotationFS, String aFeatureName)
    {
        Feature feature = aAnnotationFS.getType()
                .getFeatureByBaseName(aFeatureName + FEATURE_NAME_SCORE_SUFFIX);
        return aAnnotationFS.getDoubleValue(feature);
    }

    public static String getScoreExplanation(AnnotationFS aAnnotationFS, String aFeatureName)
    {
        Feature feature = aAnnotationFS.getType()
                .getFeatureByBaseName(aFeatureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
        return aAnnotationFS.getStringValue(feature);
    }

    public static <T extends Annotation> List<T> getPredictions(CAS aCas, Class<T> aClass)
        throws Exception
    {
        Type type = CasUtil.getType(aCas, aClass);
        Feature feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        return JCasUtil.select(aCas.getJCas(), aClass).stream()
                .filter(fs -> fs.getBooleanValue(feature)).collect(Collectors.toList());
    }

    public static List<AnnotationFS> getPredictions(CAS aCas, String aTypeName) throws Exception
    {
        Type type = CasUtil.getType(aCas, aTypeName);
        Feature feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        return CasUtil.select(aCas, type).stream().filter(fs -> fs.getBooleanValue(feature))
                .collect(Collectors.toList());
    }

}
