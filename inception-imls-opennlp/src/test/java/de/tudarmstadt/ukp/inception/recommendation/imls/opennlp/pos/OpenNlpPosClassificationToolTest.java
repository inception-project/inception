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

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2000Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfigurationPrebuilds;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.IncrementalEvaluationService;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.pos.PosAnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.playground.SpecialLoadedDataset;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.Reporter;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpPosClassificationToolTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);
    private static Dataset ds;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private final String testTimestamp = sdf.format(new Date());

    private static List<List<AnnotationObject>> totalPOSTrainingsData = null;

    private static EvaluationConfiguration testConf;

    @BeforeClass
    public static void configureTests()
        throws IOException, UIMAException
    {
        // Decide which Test Configuration to use.
        testConf = EvaluationConfigurationPrebuilds.getLimitedTrainingSetConfiguration(1000);

        totalPOSTrainingsData = loadPOSTrainingsData(testConf);
    }

    @Test
    public void testOpenNlpPosClassifier()
        throws IOException
    {
        System.err.println("\n== Testing OpenNlpPosClassifier");

        ClassifierConfiguration<TrainingParameters> classifierConf = new BaseConfiguration();
        classifierConf.setLanguage("en");

        ClassificationTool<TrainingParameters> ct = new OpenNlpPosClassificationTool();
        ct.setClassifierConfiguration(classifierConf);

        IncrementalEvaluationService eval = new IncrementalEvaluationService(ct, testConf);
        EvaluationResult results = eval.evaluateIncremental(totalPOSTrainingsData);

        Reporter.dumpResults(
                new File(testConf.getResultFolder(),
                        "OpenNlpPosClassifier_" + testTimestamp + ".html"),
                testConf, results);
    }

    public static List<List<AnnotationObject>> loadPOSTrainingsData(
            EvaluationConfiguration testConf)
        throws IOException, ResourceInitializationException
    {
        // windows hack
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            SpecialLoadedDataset sds = new SpecialLoadedDataset(loader,
                    loader.getDescription("conll2000-en"));
            ds = sds;
        }
        else {
            ds = loader.load("conll2000-en");
        }

        CollectionReaderDescription reader = createReaderDescription(Conll2000Reader.class,
                Conll2000Reader.PARAM_PATTERNS, ds.getDataFiles(), Conll2000Reader.PARAM_READ_POS,
                true, Conll2000Reader.PARAM_LANGUAGE, ds.getLanguage());

        PosAnnotationObjectLoader posLoader = new PosAnnotationObjectLoader();
        return posLoader.loadAnnotationObjects(reader);
    }
}
