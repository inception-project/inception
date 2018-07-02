package de.tudarmstadt.ukp.inception.recommendation.api.model;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class LearningRecordTest {

    @Test
    public void thatTokenTextIsTruncated() {
        char[] charArray = new char[300];
        Arrays.fill(charArray, 'X');
        String longTokenText = new String(charArray);

        LearningRecord sut = new LearningRecord();
        sut.setTokenText(longTokenText);

        assertThat(sut.getTokenText())
            .as("TokenText has been truncated")
            .hasSize(255);
    }
}