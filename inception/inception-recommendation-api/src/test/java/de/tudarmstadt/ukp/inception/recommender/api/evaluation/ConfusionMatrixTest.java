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
package de.tudarmstadt.ukp.inception.recommender.api.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.ConfusionMatrix;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;

public class ConfusionMatrixTest
{
    private List<LabelPair> instances;

    @BeforeEach
    public void setUp()
    {
        instances = new ArrayList<>();
        String[][] instanceLabels = new String[][] { { "pos", "neg" }, { "pos", "neg" },
                { "neg", "neg" }, { "pos", "pos" }, { "neutral", "pos" }, { "neutral", "neutral" },
                { "neutral", "neutral" }, { "neg", "neutral" }, { "neg", "pos" }, { "pos", "pos" },
                { "pos", "pos" }, { "neutral", "pos" }, { "neg", "pos" }, { "pos", "pos" }, };
        for (String[] labels : instanceLabels) {
            instances.add(new LabelPair(labels[0], labels[1]));
        }
    }

    @Test
    public void testIncrementCounts()
    {
        String[][] expectedKeys = { { "pos", "pos" }, { "pos", "neg" }, { "pos", "neutral" },
                { "neg", "pos" }, { "neg", "neg" }, { "neg", "neutral" }, { "neutral", "pos" },
                { "neutral", "neg" }, { "neutral", "neutral" } };
        int[] expectedCounts = { 4, 2, 0, 2, 1, 1, 2, 0, 2 };

        ConfusionMatrix matrix = new ConfusionMatrix("datapointUnit");
        instances.stream().forEach(
                pair -> matrix.incrementCounts(pair.getPredictedLabel(), pair.getGoldLabel()));

        for (int i = 0; i < expectedKeys.length; i++) {
            assertThat(matrix.getEntryCount(expectedKeys[i][1], expectedKeys[i][0]))
                    .as("has correct value").isEqualTo(expectedCounts[i]);
        }
    }

    @Test
    public void testContainsEntry()
    {
        ConfusionMatrix matrix = new ConfusionMatrix("datapointUnit");
        instances.stream().forEach(
                pair -> matrix.incrementCounts(pair.getPredictedLabel(), pair.getGoldLabel()));

        assertThat(matrix.containsEntry("pos", "pos")).as("has entry (gold: pos , pred.: pos)")
                .isTrue();
        assertThat(matrix.containsEntry("neutral", "pos"))
                .as("has entry (gold: pos , pred.: neutral)").isFalse();
    }

    @Test
    public void testAddMatrix()
    {
        String[][] expectedKeys = { { "pos", "pos" }, { "pos", "neg" }, { "pos", "neutral" },
                { "neg", "pos" }, { "neg", "neg" }, { "neg", "neutral" }, { "neutral", "pos" },
                { "neutral", "neg" }, { "neutral", "neutral" } };
        int[] expectedCounts = { 4, 2, 0, 2, 1, 2, 3, 1, 3 };
        ConfusionMatrix matrix1 = getExampleMatrix(new String[][] { { "neg", "neutral" },
                { "neutral", "pos" }, { "neutral", "neg" }, { "neutral", "neutral" } });
        ConfusionMatrix matrix2 = new ConfusionMatrix("datapointUnit");
        instances.stream().forEach(
                pair -> matrix2.incrementCounts(pair.getPredictedLabel(), pair.getGoldLabel()));

        matrix1.addMatrix(matrix2);

        for (int i = 0; i < expectedKeys.length; i++) {
            assertThat(matrix1.getEntryCount(expectedKeys[i][1], expectedKeys[i][0]))
                    .as("has correct value").isEqualTo(expectedCounts[i]);
        }
    }

    private ConfusionMatrix getExampleMatrix(String[][] aKeys)
    {
        ConfusionMatrix matrix = new ConfusionMatrix("datapointUnit");
        for (String[] key : aKeys) {
            matrix.incrementCounts(key[1], key[0]);
        }
        return matrix;
    }

}
