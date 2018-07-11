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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.TrainingParameters;

/**
 * The implementation of a trainer for a POS-Tagger using the OpenNlp library.
 * 
 *
 *
 */
public class OpenNlpPosTrainer
    extends Trainer<TrainingParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private LinkedList<List<AnnotationObject>> knownData = new LinkedList<>();
    private Object trainedModel;

    public OpenNlpPosTrainer(ClassifierConfiguration<TrainingParameters> conf)
    {
        super(conf);
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        knownData.addAll(trainingDataIncrement);

        ClassifierConfiguration<TrainingParameters> conf = getClassifierConfiguration();

        try (POSSampleStream stream = new POSSampleStream(createPOSSamples(knownData))) {
            trainedModel = POSTaggerME.train(conf.getLanguage(), stream, conf.getParams(),
                    new POSTaggerFactory(null, null));
        }
        catch (IOException e) {
            log.error("Could not train model for OpenNlpPosClassifier.", e);
        }
        return trainedModel;
    }

    private List<POSSample> createPOSSamples(List<List<AnnotationObject>> data)
    {
        List<POSSample> result = new ArrayList<>();

        for (List<AnnotationObject> sentence : data) {
            String[] tokens = new String[sentence.size()];
            String[] tags = new String[sentence.size()];

            for (int i = 0; i < sentence.size(); i++) {
                AnnotationObject ao = sentence.get(i);
                tokens[i] = ao.getCoveredText();
                tags[i] = ao.getLabel();
            }

            result.add(new POSSample(tokens, tags));
        }

        return result;
    }

    @Override
    public boolean saveModel()
    {
        File fOut = getClassifierConfiguration().getModelFile();
        if (fOut == null) {
            return false;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fOut))) {
            oos.writeObject(knownData);
            oos.writeObject(trainedModel);
            oos.close();
        }
        catch (IOException e) {
            log.error("Could not save trained model for OpenNlpPosClassifier in file {}.",
                    fOut.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object loadModel()
    {
        File fIn = getClassifierConfiguration().getModelFile();
        if (fIn == null) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fIn))) {
            Object o = ois.readObject();
            if (o instanceof LinkedList<?>) {
                knownData = (LinkedList<List<AnnotationObject>>) o;
            }

            trainedModel = ois.readObject();
            return trainedModel;
        }
        catch (IOException | ClassNotFoundException e) {
            log.error("Could not load trained model for OpenNlpPosClassifer from file {}.",
                    fIn.getAbsolutePath(), e);
            return null;
        }
    }

    @Override
    public void reconfigure()
    {
        trainedModel = null;
    }
}
