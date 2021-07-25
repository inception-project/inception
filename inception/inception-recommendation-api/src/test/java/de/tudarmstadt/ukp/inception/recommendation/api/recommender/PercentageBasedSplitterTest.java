/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;

public class PercentageBasedSplitterTest
{

    @ParameterizedTest(name = "{index}: running on data k: {0}, #train: {1}, #test: {2}")
    @MethodSource("data")
    public void thatSplittingWorks(double aK, int aTrainingSetSize, int aTestSetSize)
    {
        List<String> data = asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        PercentageBasedSplitter splitter = new PercentageBasedSplitter(aK, 4);
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

        assertThat(trainingSet).as("Training set has correct size").hasSize(aTrainingSetSize);
        assertThat(testSet).as("Test set has correct size").hasSize(aTestSetSize);
    }

    public static Collection<Object[]> data()
    {
        // k, trainingSetSize, testSetSize
        return Arrays.asList(new Object[][] { //
                { 0.1, 2, 8 }, //
                { 0.2, 2, 8 }, //
                { 0.3, 3, 7 }, //
                { 0.4, 4, 6 }, //
                { 0.5, 5, 5 }, //
                { 0.6, 6, 4 }, //
                { 0.7, 7, 3 }, //
                { 0.8, 8, 2 }, //
                { 0.9, 8, 2 } });
    }

    @Test()
    public void thatPercentageHasToBePercentage()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            PercentageBasedSplitter splitter = new PercentageBasedSplitter(42.1337, 10);
        });
    }
}
