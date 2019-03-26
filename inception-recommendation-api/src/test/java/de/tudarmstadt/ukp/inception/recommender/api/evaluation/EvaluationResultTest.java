package de.tudarmstadt.ukp.inception.recommender.api.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.AnnotatedTokenPair;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;

public class EvaluationResultTest
{
    private EvaluationResult calc;

    @Before
    public void setUp()
    {
        List<AnnotatedTokenPair> instances = new ArrayList<>();
        String[][] instanceLabels = new String[][] { { "PER", "PER" }, { "PER", "ORG" },
                { "ORG", "PER" }, { "ORG", "LOC" }, { "PER", "LOC" }, { "LOC", "ORG" },
                { "LOC", "LOC" }, { "ORG", "LOC" }, { "PER", "ORG" }, { "ORG", "ORG" },
                { "LOC", "LOC" }, { "ORG", "LOC" }, { "PER", "ORG" }, { "ORG", "ORG" },
                { "LOC", "PER" }, { "ORG", "ORG" }, { "PER", "PER" }, { "ORG", "ORG" } };
        for (String[] labels : instanceLabels) {
            instances.add(new AnnotatedTokenPair(0, 0, labels[0], labels[1]));
        }
        calc = new EvaluationResult(null, instances.stream());
    }

    @Test
    public void thatAccuracyWorks()
    {
        assertThat(calc.getAccuracyScore()).as("accuracy is correctly calculated")
                .isEqualTo(4.0 / 9.0);
    }

    @Test
    public void thatPrecisionWorks()
    {
        assertThat(calc.getPrecisionScore()).as("precision is correctly calculated")
                .isEqualTo((0.5 + 0.5 + 1.0 / 3.0) / 3);
    }

    @Test
    public void thatRecallWorks()
    {
        assertThat(calc.getRecallScore()).as("recall is correctly calculated")
                .isEqualTo((0.5 + 0.5 + 1.0 / 3.0) / 3);
    }

    @Test
    public void thatF1Works()
    {
        assertThat(calc.getF1Score()).as("f1 is correctly calculated")
                .isEqualTo(2 * (4.0 / 9.0 * 4.0 / 9.0) / (8.0 / 9.0));
    }
}
