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

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_SUFFIX;
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
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;

public class RecommenderTestHelper
{
    public static void addPredictionFeatures(CAS aCas, String aTypeName, String aFeatureName)
        throws IOException, UIMAException
    {
        var tsd = typeSystem2TypeSystemDescription(aCas.getTypeSystem());
        var td = tsd.getType(aTypeName);

        td.addFeature(FEATURE_NAME_IS_PREDICTION, "Is prediction", TYPE_NAME_BOOLEAN);

        td.addFeature(aFeatureName + FEATURE_NAME_SCORE_SUFFIX, "Score", TYPE_NAME_DOUBLE);

        td.addFeature(aFeatureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX,
                "Score explanation feature", TYPE_NAME_STRING);

        td.addFeature(aFeatureName + FEATURE_NAME_CORRECTION_SUFFIX, "Correction flag",
                TYPE_NAME_BOOLEAN);

        td.addFeature(aFeatureName + FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX,
                "Correction explanation", TYPE_NAME_STRING);

        td.addFeature(aFeatureName + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX, "Suggestion mode",
                TYPE_NAME_STRING);

        var schemaService = new AnnotationSchemaServiceImpl();
        schemaService.upgradeCas(aCas, tsd);
    }

    public static <T extends Annotation> void addPredictionFeatures(CAS aCas, Class<T> aClass,
            String aFeatureName)
        throws IOException, UIMAException
    {
        addPredictionFeatures(aCas, aClass.getName(), aFeatureName);
    }

    public static double getScore(AnnotationFS aAnnotationFS, String aFeatureName)
    {
        var feature = aAnnotationFS.getType()
                .getFeatureByBaseName(aFeatureName + FEATURE_NAME_SCORE_SUFFIX);
        return aAnnotationFS.getDoubleValue(feature);
    }

    public static String getScoreExplanation(AnnotationFS aAnnotationFS, String aFeatureName)
    {
        var feature = aAnnotationFS.getType()
                .getFeatureByBaseName(aFeatureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
        return aAnnotationFS.getStringValue(feature);
    }

    public static <T extends Annotation> List<T> getPredictions(CAS aCas, Class<T> aClass)
        throws Exception
    {
        var type = CasUtil.getType(aCas, aClass);
        var feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        return aCas.select(aClass) //
                .filter(fs -> fs.getBooleanValue(feature)) //
                .collect(Collectors.toList());
    }

    public static List<AnnotationFS> getPredictions(CAS aCas, String aTypeName) throws Exception
    {
        var type = CasUtil.getType(aCas, aTypeName);
        var feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        return aCas.<Annotation> select(type) //
                .filter(fs -> fs.getBooleanValue(feature)) //
                .collect(Collectors.toList());
    }

    public static List<FeatureStructure> getPredictionFSes(CAS aCas, String aTypeName)
        throws Exception
    {
        var type = CasUtil.getType(aCas, aTypeName);
        var feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        return aCas.select(type) //
                .filter(fs -> fs.getBooleanValue(feature)) //
                .collect(Collectors.toList());
    }
}
