package de.tudarmstadt.ukp.inception.recommender.api.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.AnnotatedTokenPair;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.ConfusionMatrix;

public class ConfusionMatrixTest
{
    private List<AnnotatedTokenPair> instances;
    
    @Before
    public void setUp()
    {
        instances = new ArrayList<>();
        String[][] instanceLabels = new String[][] { { "pos", "neg" }, { "pos", "neg" },
                { "neg", "neg" }, { "pos", "pos" }, { "neutral", "pos" }, { "neutral", "neutral" },
                { "neutral", "neutral" }, { "neg", "neutral" }, { "neg", "pos" }, { "pos", "pos" },
                { "pos", "pos" }, { "neutral", "pos" }, { "neg", "pos" }, { "pos", "pos" },};
        for (String[] labels : instanceLabels) {
            instances.add(new AnnotatedTokenPair(labels[0], labels[1]));
        }
    }

    @Test
    public void testIncrementCounts()
    {
        String[][] expectedKeys = { { "pos", "pos" }, { "pos", "neg" }, { "pos", "neutral" },
                { "neg", "pos" }, { "neg", "neg" }, { "neg", "neutral" }, { "neutral", "pos" },
                { "neutral", "neg" }, { "neutral", "neutral" } };
        int[] expectedCounts = { 4, 2, 0, 2, 1, 1, 2, 0, 2 };

        ConfusionMatrix matrix = new ConfusionMatrix();
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
        ConfusionMatrix matrix = new ConfusionMatrix();
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
        ConfusionMatrix matrix2 = new ConfusionMatrix();
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
        ConfusionMatrix matrix = new ConfusionMatrix();
        for (String[] key : aKeys) {
            matrix.incrementCounts(key[1], key[0]);
        }
        return matrix;
    }

}
