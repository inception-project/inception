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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.api.util.CasUtil;

/**
 * The implementation of a POS-Tagger using the Margin Infused Relaxed Algorithm (MIRA).
 * 
 *
 *
 */
public class MiraPosClassifier
    extends Classifier<MiraConfigurationParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private IncrementalMira mira = new IncrementalMira();
    
    private boolean modelLoaded = false;

    public MiraPosClassifier(ClassifierConfiguration<MiraConfigurationParameters> conf)
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
            mira.loadModel(fIn.getAbsolutePath());
            modelLoaded = true;
        }
        catch (ClassNotFoundException | IOException e) {
            log.error("Expected model type: File - but was: [{}]",
                    aModel != null ? aModel.getClass() : aModel);
        }
    }

    @Override
    public <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
            List<List<T>> inputData)
    {
        if (inputData == null) {
            log.warn("No input data");
            return new ArrayList<>();
        }

        if (!modelLoaded) {
            log.warn("No model has been set.");
            return new ArrayList<>();
        }
        
        try {
            String feature = conf.getFeature();
            if (feature == null) {
                feature = "PosValue";
            }
            return mira.test(CasUtil.transformToAnnotationObjects(inputData, feature, "MiraPosClassifier"));
        }
        catch (IOException e) {
            log.error("Cannot predict sentences.", e);
        }
        return new ArrayList<>();
    }

    @Override
    public void reconfigure()
    {
        // Nothing todo here the configuration comes out of the model.
    }
}
