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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

/**
 * The implementation of a Named Entity Recognizer using the OpenNlp library.
 * 
 *
 *
 */
public class OpenNlpNerClassifier extends Classifier<TrainingParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private TokenNameFinderModel model;
    
    public OpenNlpNerClassifier(ClassifierConfiguration<TrainingParameters> conf)
    {
        super(conf);
    }
    
    @Override
    public void setModel(Object aModel)
    {
        if (aModel instanceof TokenNameFinderModel) {
            model = (TokenNameFinderModel) aModel;
        } else {
            log.error("Expected model type: TokenNameFinderModel - but was: [{}]",
                    aModel != null ? aModel.getClass() : aModel);
        }
        
    }

    @Override
    public <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
            List<List<T>> inputData)
    {
        List<List<List<AnnotationObject>>> result = new LinkedList<>();
        
        if (inputData == null || inputData.isEmpty()) {
            log.warn("No input data.");
            return result;
        }

        if (model == null) {
            log.warn("No model set.");
            return result;
        }

        NameFinderME finder = new NameFinderME(model);
        
        int id = 0;
        
        String feature = conf.getFeature();
        if (feature == null) {
            feature = "value";
        }
                
        for (List<T> sentence : inputData) {
            
            String[] tokenArr = new String[sentence.size()];

            for (int i = 0; i < tokenArr.length; i++) {
                tokenArr[i] = sentence.get(i).getCoveredText();
            }

            Span[] generatedNames = finder.find(tokenArr);
            double[] confidence = finder.probs();
            
            List<List<AnnotationObject>> annotatedSentence = new LinkedList<>();
            
            for (int i = 0; i < sentence.size(); i++) {
                T t = sentence.get(i);
                List<AnnotationObject> word = new LinkedList<>();
                word.add(new AnnotationObject(t, id, feature,
                        "OpenNlpNerClassifier", confidence[i], conf.getRecommenderId()));
                id++;
                annotatedSentence.add(word);
            }

            for (Span span : generatedNames) {
                for (int index = span.getStart(); index < span.getEnd(); index++) {
                    annotatedSentence.get(index).get(0).setLabel(span.getType());
                }
            }
            result.add(annotatedSentence);
        }
        
        return result;
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here. The current configuration is used for every prediction.
    }
}
