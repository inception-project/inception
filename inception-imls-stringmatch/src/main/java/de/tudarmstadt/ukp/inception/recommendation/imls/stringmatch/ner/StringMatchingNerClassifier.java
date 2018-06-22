/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.ner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;

/**
 * The implementation of a Named Entity Recognizer using simple String matching.
 */
public class StringMatchingNerClassifier
    extends Classifier<Object>
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<List<String>, String> trainedModel;

    public StringMatchingNerClassifier(ClassifierConfiguration<Object> conf)
    {
        super(conf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setModel(Object aModel)
    {
        if (aModel instanceof Map) {
            trainedModel = (HashMap<List<String>, String>) aModel;
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
        List<List<List<AnnotationObject>>> result = new ArrayList<>();

        if (inputData == null) {
            log.warn("No input data");
            return result;
        }

        if (trainedModel == null) {
            log.warn("No model has been set.");
            return result;
        }

        int tokenId = 0;

        String feature = conf.getFeature();
        if (feature == null) {
            feature = "value";
        }

        for (List<T> sentence : inputData) {
            List<List<AnnotationObject>> annotatedSentence = new ArrayList<>();
            for (T token : sentence) {
                List<AnnotationObject> word = new ArrayList<>();
                word.add(new AnnotationObject(token, tokenId, feature,
                        "StringMatchingNerClassifier", conf.getRecommenderId()));
                tokenId++;
                annotatedSentence.add(word);
            }

            predictSentence(annotatedSentence);
            
            result.add(annotatedSentence);
        }
        
        return result;
    }

    public void predictSentence(List<List<AnnotationObject>> sentence)
    {
        // Iterate over all the learned (multi-word)/label combinations
        for (Entry<List<String>, String> entry : trainedModel.entrySet()) {
            List<String> neTokens = entry.getKey();

            for (int i = 0; i < (sentence.size() - neTokens.size()); i++) {
                boolean hit = true;

                for (int j = 0; j < neTokens.size(); j++) {
                    if (!sentence.get(i + j).get(0).getCoveredText().equals(neTokens.get(j))) {
                        hit = false;
                        break;
                    }
                }

                if (hit) {
                    for (int j = 0; j < neTokens.size(); j++) {
                        sentence.get(i + j).get(0).setLabel(entry.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void reconfigure()
    {
        // nothing to do here atm.
    }
}
