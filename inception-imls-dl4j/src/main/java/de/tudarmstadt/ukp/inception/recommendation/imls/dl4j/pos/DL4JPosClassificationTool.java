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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos.PosAnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos.BaseConfiguration;

/**
 * Implementation of POS-Tagging using the DL4J library to build a multi-Layer neural net. 
 */
public class DL4JPosClassificationTool
    extends ClassificationTool<DL4JConfigurationParameters>
{
    private static final Logger logger = LoggerFactory.getLogger(DL4JPosClassificationTool.class);

    public DL4JPosClassificationTool(long aRecommenderId, String feature) {
        this(aRecommenderId, getCache(), feature, null);
    }

    public DL4JPosClassificationTool(long aRecommenderId, String feature, AnnotationLayer aLayer) {
        this(aRecommenderId, getCache(), feature, aLayer);
    }
    
    public static File getCache() {
        File folder = new File("target/cache/");
        folder.mkdirs();
        return folder;
    }
            
    public DL4JPosClassificationTool(long aRecommenderId, File aCache, String feature,
        AnnotationLayer aLayer)
    {
        super();
        DatasetFactory loader = new DatasetFactory(aCache);
        Dataset ds = null;
        ClassifierConfiguration<DL4JConfigurationParameters> conf = new BaseConfiguration(
            aRecommenderId);
        try {
            ds = loader.load("glove.6B.50d.dl4jw2v");
            // ds = loader.load("glove.6B.100d.dl4jw2v");
            // ds = loader.load("glove.6B.200d.dl4jw2v");
            // ds = loader.load("glove.6B.300d.dl4jw2v");
            conf.getParams().createWordVectors(ds.getDataFiles()[0]);
        }
        catch (IOException e) {
            logger.error("Cannot load word vectors.", e);
        }
        conf.setLanguage("en");

        setName(DL4JPosClassificationTool.class.getName());
        setTrainer(new DL4JPosTrainer(conf));
        setClassifier(new DL4JPosClassifier(conf));
        setLoader(new PosAnnotationObjectLoader(aLayer, feature));
        setTrainOnCompleteSentences(true);
    }
}
