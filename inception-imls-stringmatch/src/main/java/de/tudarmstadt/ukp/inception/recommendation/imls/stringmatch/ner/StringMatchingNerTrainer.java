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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.ner.NerDataHelper;

/**
 * The implementation of a trainer for a Named Entity Recognizer using simple String matching.
 * 
 *
 *
 */
public class StringMatchingNerTrainer
    extends Trainer<Object>
{
    private Logger log = LoggerFactory.getLogger(getClass());

    HashMap<List<String>, String> trainedModel;

    public StringMatchingNerTrainer(ClassifierConfiguration<Object> conf)
    {
        super(conf);
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        if (trainedModel == null) {
            reconfigure();
        }

        for (List<AnnotationObject> sentence : trainingDataIncrement) {
            trainedModel.putAll(NerDataHelper.getAnnotationMappingForSentence(sentence));
        }

        return trainedModel;
    }

    @Override
    public boolean saveModel()
    {
        File fOut = getClassifierConfiguration().getModelFile();
        if (fOut == null) {
            return false;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fOut))) {
            oos.writeObject(trainedModel);
            oos.close();
        }
        catch (IOException e) {
            log.error("Could not save trained model for StringMatchingNerTrainer in file {}.",
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
            if (o instanceof HashMap) {
                trainedModel = (HashMap<List<String>, String>) o;
                return trainedModel;
            }
            return o;
        }
        catch (IOException | ClassNotFoundException e) {
            log.error("Could not load trained model for StringMatchingNerTrainer from file {}.",
                            fIn.getAbsolutePath(), e);
            return null;
        }
    }

    @Override
    public void reconfigure()
    {
        trainedModel = new HashMap<>();
    }

}
