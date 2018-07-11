/*

 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.pos;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;

/**
 * Implementation of POS-Tagging using simple String matching.
 */
public class StringMatchingPosClassifier
    extends Classifier<Object>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    Map<String, String> annotationMapping = new HashMap<>();

    public StringMatchingPosClassifier(ClassifierConfiguration<Object> conf)
    {
        super(conf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setModel(Object aModel)
    {
        if (aModel instanceof Map) {
            annotationMapping = (Map<String, String>) aModel;
        }
        else {
            log.error("Expected model type: Map<String,String> - but was: [{}]",
                    aModel != null ? aModel.getClass() : aModel);
        }
    }

    @Override
    public <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
            List<List<T>> inputData)
    {
        List<List<List<AnnotationObject>>> result = new LinkedList<>();

        if (inputData == null) {
            log.warn("No input data");
            return result;
        }

        if (annotationMapping == null) {
            log.warn("No model has been set.");
            return result;
        }

        int id = 0;
        String feature = conf.getFeature();
        if (feature == null) {
            feature = "PosValue";
        }

        for (List<T> sentence : inputData) {

            List<List<AnnotationObject>> annotatedSentence = new LinkedList<>();
            
            for (int i = 0; i < sentence.size(); i++) {

                List<AnnotationObject> word = new LinkedList<>();
                T t = sentence.get(i);

                AnnotationObject ao = new AnnotationObject(t,
                    annotationMapping.get(t.getCoveredText()), null, id, feature,
                    "StringMatchingPosClassifier", conf.getRecommenderId());
                word.add(ao);
                id++;
                annotatedSentence.add(word);
            }
            result.add(annotatedSentence);
        }

        return result;
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here atm.
    }
}
