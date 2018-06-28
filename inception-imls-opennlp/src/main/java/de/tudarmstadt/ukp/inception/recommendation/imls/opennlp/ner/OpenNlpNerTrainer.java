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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.ner.NerDataHelper;
import opennlp.tools.namefind.BioCodec;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;


/**
 * The implementation of a trainer for a Named Entity Recognizer using the OpenNlp library.
 * 
 *
 *
 */
public class OpenNlpNerTrainer
    extends Trainer<TrainingParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private LinkedList<List<AnnotationObject>> knownData = new LinkedList<>();
    private Object trainedModel;

    public OpenNlpNerTrainer(ClassifierConfiguration<TrainingParameters> conf)
    {
        super(conf);
    }

    private List<NameSample> createNameSamples(List<List<AnnotationObject>> data)
    {
        List<NameSample> result = new ArrayList<>();

        for (List<AnnotationObject> sentence : data) {
            List<List<AnnotationObject>> namedEntities = NerDataHelper.getNamedEntities(sentence);

            List<Span> spans = new LinkedList<>();
            for (List<AnnotationObject> namedEntity : namedEntities) {
                AnnotationObject aoFirst = namedEntity.get(0);
                AnnotationObject aoLast = namedEntity.get(namedEntity.size() - 1);
                Span span = new Span(aoFirst.getOffset().getBeginToken(),
                        aoLast.getOffset().getEndToken() + 1, aoFirst.getLabel());
                spans.add(span);
            }

            Span[] spanArray = new Span[spans.size()];
            spans.toArray(spanArray);

            String[] tokens = new String[sentence.size()];
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = sentence.get(i).getCoveredText();
            }

            result.add(new NameSample(tokens, spanArray, true));
        }

        return result;
    }

    private byte[] loadFeatureGen(File aFile)
        throws ResourceInitializationException
    {
        byte[] featureGenCfg = null;
        if (aFile != null) {
            try (InputStream in = new FileInputStream(aFile)) {
                featureGenCfg = IOUtils.toByteArray(in);
            }
            catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
        return featureGenCfg;
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        knownData.addAll(trainingDataIncrement);

        try (NameSampleStream stream = new NameSampleStream(
                createNameSamples(trainingDataIncrement))) {

            byte[] featureGenCfg;
            featureGenCfg = loadFeatureGen(null);

            ClassifierConfiguration<TrainingParameters> conf = getClassifierConfiguration();

            return NameFinderME.train(conf.getLanguage(), null, stream, conf.getParams(),
                    new TokenNameFinderFactory(featureGenCfg,
                            Collections.<String, Object>emptyMap(), new BioCodec()));
        }
        catch (Exception e) {
            log.error("Exception during training the OpenNLP Named Entity Recognizer model.", e);
            return null;
        }
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
            log.error("Could not save trained model for OpenNlpNerClassifier in file {}.",
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
            log.error("Could not load trained model for OpenNlpNerClassifier from file {}.",
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
