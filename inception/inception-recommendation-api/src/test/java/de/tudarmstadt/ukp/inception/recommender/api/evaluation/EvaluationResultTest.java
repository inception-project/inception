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

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;

public class EvaluationResultTest
{
    private List<LabelPair> instances;

    @BeforeEach
    public void setUp()
    {
        instances = new ArrayList<>();
        String[][] instanceLabels = new String[][] { { "PER", "PER" }, { "PER", "ORG" },
                { "ORG", "PER" }, { "ORG", "LOC" }, { "PER", "LOC" }, { "LOC", "ORG" },
                { "LOC", "LOC" }, { "ORG", "LOC" }, { "PER", "ORG" }, { "ORG", "ORG" },
                { "LOC", "LOC" }, { "ORG", "LOC" }, { "PER", "ORG" }, { "ORG", "ORG" },
                { "LOC", "PER" }, { "ORG", "ORG" }, { "PER", "PER" }, { "ORG", "ORG" } };
        for (String[] labels : instanceLabels) {
            instances.add(new LabelPair(labels[0], labels[1]));
        }
    }

    @Test
    public void thatAccuracyWorks()
    {
        EvaluationResult calc = instances.stream().collect(EvaluationResult.toEvaluationResult());

        assertThat(calc.computeAccuracyScore()).as("accuracy is correctly calculated")
                .isEqualTo(4.0 / 9.0);
    }

    @Test
    public void thatPrecisionWorks()
    {
        EvaluationResult calc = instances.stream().collect(EvaluationResult.toEvaluationResult());

        assertThat(calc.computePrecisionScore()).as("precision is correctly calculated")
                .isEqualTo((0.5 + 0.5 + 1.0 / 3.0) / 3);
    }

    @Test
    public void thatRecallWorks()
    {
        EvaluationResult calc = instances.stream().collect(EvaluationResult.toEvaluationResult());

        assertThat(calc.computeRecallScore()).as("recall is correctly calculated")
                .isEqualTo((0.5 + 0.5 + 1.0 / 3.0) / 3);
    }

    @Test
    public void thatF1Works()
    {
        EvaluationResult calc = instances.stream().collect(EvaluationResult.toEvaluationResult());

        assertThat(calc.computeF1Score()).as("f1 is correctly calculated")
                .isEqualTo(2 * (4.0 / 9.0 * 4.0 / 9.0) / (8.0 / 9.0));
    }

    @Test
    public void thatIgnoringALabelWorks()
    {
        double expectedPrec = (4.0 / 5 + 2.0 / 5) * 0.5;
        double expectedRecall = 0.5;
        EvaluationResult calc = instances.stream()
                .collect(toEvaluationResult("", "", 0, 0, 0, "PER"));

        assertThat(calc.computeF1Score()).as("f1 with ignore label is correctly calculated")
                .isEqualTo(2 * expectedPrec * expectedRecall / (expectedPrec + expectedRecall));
        assertThat(calc.computeRecallScore()).as("recall with ignore label is correctly calculated")
                .isEqualTo(expectedRecall);
        assertThat(calc.computeAccuracyScore())
                .as("accuracy with ignore label is correctly calculated").isEqualTo(6.0 / 12);
        assertThat(calc.computePrecisionScore())
                .as("precision with ignore label is correctly calculated").isEqualTo(expectedPrec);
    }

    @Test
    public void thatNumOfLabelsWorks()
    {
        EvaluationResult calc = instances.stream().collect(toEvaluationResult());
        assertThat(calc.getNumOfLabels()).as("check num of labels for no ignoreLabel").isEqualTo(3);

        calc = instances.stream().collect(toEvaluationResult("", "", 0, 0, 0, "PER"));
        assertThat(calc.getNumOfLabels()).as("check num of labels for one ignoreLabel")
                .isEqualTo(2);

        calc = instances.stream().collect(toEvaluationResult("", "", 0, 0, 0, "PER", "ORG"));
        assertThat(calc.getNumOfLabels()).as("check num of labels for two ignoreLabel")
                .isEqualTo(1);
    }

    @Test
    public void thatMissingClassesWorks()
    {
        // test with classes which are never gold or never predicted
        List<LabelPair> newInstances = new ArrayList<>(instances);
        newInstances.add(new LabelPair("PART", "ORG"));
        newInstances.add(new LabelPair("PER", "PUNC"));

        EvaluationResult calc = newInstances.stream()
                .collect(EvaluationResult.toEvaluationResult());

        assertThat(calc.computeAccuracyScore())
                .as("accuracy with missing classes is correctly calculated").isEqualTo(2.0 / 5);
        assertThat(calc.computePrecisionScore())
                .as("precision with missing classes is correctly calculated")
                .isEqualTo((0.5 + 7.0 / 9) / 5);
        assertThat(calc.computeRecallScore())
                .as("recall with missing classes is correctly calculated")
                .isEqualTo((2.0 / 7 + 0.5 + 0.5) / 5);
        assertThat(calc.computeF1Score()).as("f1 with missing classes is correctly calculated")
                .isEqualTo(2 * ((0.5 + 7.0 / 9) / 5 * (2.0 / 7 + 0.5 + 0.5) / 5)
                        / ((0.5 + 7.0 / 9) / 5 + (2.0 / 7 + 0.5 + 0.5) / 5));
    }
}
