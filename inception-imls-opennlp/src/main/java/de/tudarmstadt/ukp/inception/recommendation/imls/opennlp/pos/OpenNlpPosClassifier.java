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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classifier.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.CasUtil;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.TrainingParameters;

/**
 * Implementation of POS-Tagging using the OpenNlp library.
 * 
 *
 *
 */
public class OpenNlpPosClassifier
    extends Classifier<TrainingParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private POSModel model;

    public OpenNlpPosClassifier(ClassifierConfiguration<TrainingParameters> conf)
    {
        super(conf);
    }

    @Override
    public void setModel(Object aModel)
    {
        if (aModel instanceof POSModel) {
            model = (POSModel) aModel;
        }
        else {
            log.error("Expected model type: POSModel - but was: [{}]",
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

        if (model == null) {
            log.warn("No model has been set.");
            return result;
        }

        POSTaggerME tagger = new POSTaggerME(model);
        
        int id = 0;

        String feature = conf.getFeature();
        if (feature == null) {
            feature = "PosValue";
        }

        for (List<T> sentence : inputData) {

            String[] tokenArr = CasUtil.getCoveredTexts(sentence);
            
            opennlp.tools.util.Sequence[] bestSequences = tagger.topKSequences(tokenArr);
            double[][] confidence = new double[bestSequences.length][tokenArr.length];

            List<List<AnnotationObject>> sentencePredictions = new LinkedList<>();  
            
            for (int j = 0; j < bestSequences[0].getOutcomes().size(); j++) {
                
                List<AnnotationObject> wordPredictions = new LinkedList<>();
                
                for (int i = 0; i < bestSequences.length; i++) {
                    confidence[i] = bestSequences[i].getProbs();

                    AnnotationObject ao = new AnnotationObject(
                            bestSequences[i].getOutcomes().get(j), sentence.get(j), sentence, id,
                            feature, "OpenNlpPosClassifier", confidence[i][j]);
                    id++;
                    wordPredictions.add(ao);
                }
                
                sentencePredictions.add(wordPredictions);
            }
            
            result.add(sentencePredictions);
        }

        return result;
    }

    @Override
    public void setUser(User user)
    {
        // Not needed.
    }

    @Override
    public void setProject(Project project)
    {
        // Not needed.
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here atm.
    }
}
