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
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfigurationPrebuilds;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.IncrementalEvaluationService;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.ner.NerAnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.Reporter;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerClassificationToolTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);
    private static Dataset ds;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private final String testTimestamp = sdf.format(new Date());

    private static List<List<AnnotationObject>> totalNETrainingsData = null;

    private static EvaluationConfiguration testConf;

    @BeforeClass
    public static void configureTests()
        throws IOException, UIMAException
    {
        // Decide which Test Configuration to use.
        testConf = EvaluationConfigurationPrebuilds.getLimitedTrainingSetConfiguration(1000);

        totalNETrainingsData = loadNETrainingsData(testConf);
    }

    @Test
    public void testOpenNlpNerClassifier()
        throws IOException
    {
        System.err.println("\n== Testing OpenNlpNerClassifier");

        ClassifierConfiguration<TrainingParameters> classifierConf = new BaseConfiguration();
        classifierConf.setLanguage("de");

        ClassificationTool<TrainingParameters> ct = new OpenNlpNerClassificationTool();
        ct.setClassifierConfiguration(classifierConf);

        IncrementalEvaluationService eval = new IncrementalEvaluationService(ct, testConf);
        EvaluationResult results = eval.evaluateIncremental(totalNETrainingsData);

        Reporter.dumpResults(
                new File(testConf.getResultFolder(),
                        "OpenNlpNerClassifier" + testTimestamp + ".html"),
                testConf, results);
    }
    
    public static List<List<AnnotationObject>> loadNETrainingsData(
            EvaluationConfiguration testConf) throws IOException, ResourceInitializationException
    {
        ds = loader.load("germeval2014-de");
        CollectionReaderDescription reader = createReaderDescription(Conll2002Reader.class,
                Conll2002Reader.PARAM_PATTERNS, ds.getDataFiles(), Conll2002Reader.PARAM_LANGUAGE,
                ds.getLanguage(), Conll2002Reader.PARAM_COLUMN_SEPARATOR,
                Conll2002Reader.ColumnSeparators.TAB.getName(),
                Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, Conll2002Reader.PARAM_HAS_HEADER,
                true, Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);
//                Conll2002Reader.PARAM_READ_NAMED_ENTITY, true);

        NerAnnotationObjectLoader nerLoader = new NerAnnotationObjectLoader();
        return nerLoader.loadAnnotationObjects(reader);
    }
}
