/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;

@RunWith(Enclosed.class)
public class PercentageBasedSplitterTest {

    @RunWith(Parameterized.class)
    public static class ParameterizedTests {
        private final double k;
        private final int trainingSetSize;
        private final int testSetSize;

        public ParameterizedTests(double aK, int aTrainingSetSize, int aTestSetSize) {
            k = aK;
            trainingSetSize = aTrainingSetSize;
            testSetSize = aTestSetSize;
        }

        @Test
        public void thatSplittingWorks() {
            List<String> data = asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
            PercentageBasedSplitter splitter = new PercentageBasedSplitter(k, 4);
            List<String> trainingSet = new ArrayList<>();
            List<String> testSet = new ArrayList<>();

            for (String s : data) {
                switch (splitter.getTargetSet(s)) {
                case TRAIN:
                    trainingSet.add(s);
                    break;
                case TEST:
                    testSet.add(s);
                    break;
                default:
                    // Do nothing
                    break;
                }
            }

            assertThat(trainingSet)
                .as("Training set has correct size")
                .hasSize(trainingSetSize);
            assertThat(testSet)
                .as("Test set has correct size")
                .hasSize(testSetSize);
        }

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            // k, trainingSetSize, testSetSize
            return Arrays.asList(new Object[][]{
                {0.1, 2, 8},
                {0.2, 2, 8},
                {0.3, 3, 7},
                {0.4, 4, 6},
                {0.5, 5, 5},
                {0.6, 6, 4},
                {0.7, 7, 3},
                {0.8, 8, 2},
                {0.9, 8, 2}
            });
        }
    }

    @RunWith(JUnit4.class)
    public static class NonParameterizedTests {
        @Test(expected = IllegalArgumentException.class)
        public void thatPercentageHasToBePercentage() {
            PercentageBasedSplitter splitter = new PercentageBasedSplitter(42.1337, 10);
        }
    }
}
