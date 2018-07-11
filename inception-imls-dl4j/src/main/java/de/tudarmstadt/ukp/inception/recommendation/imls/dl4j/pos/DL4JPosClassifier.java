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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.api.util.CasUtil;

/**
 * The implementation of a POS-Tagger using the DL4J library and a multi layer neural network. 
 *
 *
 */
public class DL4JPosClassifier
    extends Classifier<DL4JConfigurationParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private MultiLayerNetwork net = null;
    private Vectorizer vectorizer = new Vectorizer();
    private List<String> tagset = new ArrayList<>();
    
    public DL4JPosClassifier(ClassifierConfiguration<DL4JConfigurationParameters> conf)
    {
        super(conf);
    }

    @Override
    public void setModel(Object aModel)
    {
        File fIn = null;

        if (aModel instanceof File) {
            fIn = (File) aModel;
        }
        else if (aModel instanceof String) {
            fIn = new File(aModel.toString());
        }

        if (fIn == null) {
            log.error("Expected model type: File or String - but was: [{}]",
                    aModel != null ? aModel.getClass() : aModel);
            return;
        }

        try {
            net = ModelSerializer.restoreMultiLayerNetwork(fIn);
            net.init();
            tagset = loadTagset();
        }
        catch (IOException e) {
            log.error("Cannot restore neuronal net for {}.", getClass());
        }
    }
    
    private List<String> loadTagset()
    {
        List<String> result = new ArrayList<>();
        
        DL4JConfigurationParameters params = getClassifierConfiguration().getParams();

        if (params == null) {
            log.error("Cannot load tagset. No ClassifierConfigurationParameters defined!");
            return result;
        }

        try (ZipFile archive = new ZipFile(params.getTagsetFile())) {
            ZipEntry entry = archive.getEntry(params.getTagsetEntryName());                    
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8))) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    result.add(line);
                }
            }
        }
        catch (IOException e) {
            log.error("Cannot load tagset.", e);
        }

        return result;
    }
    
    @Override
    public <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
            List<List<T>> inputData)
    {
        List<List<List<AnnotationObject>>> result = new ArrayList<>();
        
        if (inputData == null || inputData.isEmpty()) {
            log.warn("No input data.");
            return result;
        }

        if (net == null) {
            log.warn("No model set.");
            return result;
        }
        
        DL4JConfigurationParameters params = getClassifierConfiguration().getParams();
        DataSet evaluationData;

        int id = 0;
        for (int s = 0; s < inputData.size(); s++) {
            // Vectorize
            try {
                evaluationData = 
                        vectorizer.vectorize(CasUtil.transformToAnnotationObjects
                                (Arrays.asList(inputData.get(s)), "PosValue", "DL4JPosClassifier",
                                    conf.getRecommenderId()),
                                params.getWordVectors(),
                        params.getTruncateLength(), params.getMaxTagsetSize(), true);
            }
            catch (Exception e) {
                log.error("Cannot vectorize input data [sentence #{}].", s, e);
                return result;
            }

            // Predict labels
            INDArray predicted = net.output(evaluationData.getFeatureMatrix(), false,
                    evaluationData.getFeaturesMaskArray(), evaluationData.getLabelsMaskArray());

            // create predicted data
            List<T> sentence = inputData.get(s);
            List<List<AnnotationObject>> generatedSentence = new LinkedList<>();
            INDArray argMax = Nd4j.argMax(predicted, 1);

            String feature = conf.getFeature();
            if (feature == null) {
                feature = "PosValue";
            }
            
            for (int i = 0; i < sentence.size(); i++) {
                int tagIdx = argMax.getInt(i);
                T t = sentence.get(i);
                AnnotationObject ao = new AnnotationObject(tagset.get(tagIdx), t,
                        id, feature, "DL4JPosClassifier", conf.getRecommenderId());
                List<AnnotationObject> word = new LinkedList<>();
                word.add(ao);
                generatedSentence.add(word);
                id++;
            }

            result.add(generatedSentence);
        }

        return result;
    }

    @Override
    public void reconfigure()
    {
        // Nothing to do here atm. The net is configured in each predicition run.
    }
}
