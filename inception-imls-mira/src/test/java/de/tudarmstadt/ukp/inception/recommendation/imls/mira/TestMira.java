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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.uima.UIMAException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import edu.lium.mira.Mira;

public class TestMira
{
    @Ignore("Fails with OutOfMemoryError")
    @Test
    public void test()
        throws IOException, ClassNotFoundException, UIMAException
    {
        System.out.println(Runtime.getRuntime().maxMemory());
        
        File cache = DkproTestContext.getCacheFolder();
        File targetFolder = testContext.getTestOutputFolder();

        DatasetFactory loader = new DatasetFactory(cache);
        Dataset ds = loader.load("conll2000-en");

        Mira mira = new Mira();
        boolean randomInit = false;
        String trainName = ds.getDefaultSplit().getTrainingFiles()[0].getPath();
        String testName = ds.getDefaultSplit().getTestFiles()[0].getPath();
        String outputName = "testOut.txt";
        int iterations = 10;
        mira.loadTemplates("resources/imls/conf/mira/pos/pos-simple.template");
        mira.setClip(1);
        mira.maxPosteriors = false;
        mira.beamSize = 0;
        int numExamples = mira.count(trainName, 2); // absolutely necessary otherwise it does not
                                                    // work!! because this creates the labels and
                                                    // weights and so on.
        mira.initModel(randomInit);

        for (int i = 0; i < iterations; i++) {
            mira.train(trainName, iterations, numExamples, i);
            mira.averageWeights(iterations * numExamples);
        }
        mira.saveModel("miraModel.bin");
        mira.saveTextModel("miraModel.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(new File(testName)))) {
            mira.test(br, new PrintStream(new FileOutputStream(new File(outputName))));
        }

        // CollectionReaderDescription reader = createReaderDescription(
        // Conll2000Reader.class,
        // Conll2000Reader.PARAM_PATTERNS, new File(testName),
        // Conll2000Reader.PARAM_LANGUAGE, "en",
        // Conll2000Reader.PARAM_READ_POS, true);
        //
        // List<Span<String>> expected = EvalUtil.loadSamples(reader, POS.class, (pos) -> {
        // return pos.getPosValue();
        // });
        //
        // reader = createReaderDescription(
        // Conll2000Reader.class,
        // Conll2000Reader.PARAM_PATTERNS, new File(outputName),
        // Conll2000Reader.PARAM_LANGUAGE, "en",
        // Conll2000Reader.PARAM_READ_POS, true);
        //
        // List<Span<String>> generated = EvalUtil.loadSamples(reader, POS.class, (pos) -> {
        // return pos.getPosValue();
        // });
        //
        // EvalUtil.dumpResults(new File("."), expected, generated);
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
