/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.inception.recommendation.api.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.util.CasUtil; 

public class CustomAnnotationObjectLoader
    implements AnnotationObjectLoader
{

    private final String featureName;
    private final String typeName;

    public CustomAnnotationObjectLoader(String aFeatureName, String aTypeName)
    {
        featureName = aFeatureName;
        typeName = aTypeName;
    }

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjects(JCas aJCas, long aRecommenderId)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();

        if (aJCas == null) {
            return result;
        }
        
        CAS cas = aJCas.getCas();
        Type annotationType = org.apache.uima.fit.util.CasUtil.getType(cas , typeName);
        Feature feature = annotationType.getFeatureByBaseName(featureName);
        
        result = CasUtil.loadCustomAnnotatedSentences(aJCas, annotationType, feature, aRecommenderId);

        return result;
    }

    @Override
    public List<List<AnnotationObject>> loadAnnotationObjectsForTesting(JCas aJCas)
    {
        throw new UnsupportedOperationException("Write unit tests please!");
    }
}
