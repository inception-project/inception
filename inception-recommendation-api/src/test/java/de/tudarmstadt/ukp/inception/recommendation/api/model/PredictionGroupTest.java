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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionGroup.Delta;

public class PredictionGroupTest
{
    @Test
    public void thatAddingElementsToGroupWorks()
    {
        AnnotationObject rec1Sug1 = new AnnotationObject(1, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "a", "A", "#A", 0.1);
        AnnotationObject rec1Sug2 = new AnnotationObject(2, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "b", "B", "#B", 0.2);
        AnnotationObject rec2Sug1 = new AnnotationObject(3, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "c", "C", "#C", 0.1);
        AnnotationObject rec2Sug2 = new AnnotationObject(4, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "d", "D", "#D", 0.3);

        // Ensure that group grows and that all elements are added properly
        PredictionGroup sut = new PredictionGroup();
        sut.add(rec1Sug1);
        assertThat(sut).hasSize(1);
        assertThat(sut).contains(rec1Sug1);
        sut.add(rec1Sug2);
        assertThat(sut).hasSize(2);
        assertThat(sut).contains(rec1Sug2);
        sut.add(rec2Sug1);
        assertThat(sut).hasSize(3);
        assertThat(sut).contains(rec2Sug1);
        sut.add(rec2Sug2);
        assertThat(sut).hasSize(4);
        assertThat(sut).contains(rec2Sug2);
    }    
    
    @Test
    public void thatSortingWorks()
    {
        AnnotationObject rec1Sug1 = new AnnotationObject(1, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "a", "A", "#A", 0.1);
        AnnotationObject rec1Sug2 = new AnnotationObject(2, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "b", "B", "#B", 0.2);
        AnnotationObject rec2Sug1 = new AnnotationObject(3, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "c", "C", "#C", 0.1);
        AnnotationObject rec2Sug2 = new AnnotationObject(4, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "d", "D", "#D", 0.3);

        PredictionGroup sut = new PredictionGroup(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);
        
        assertThat(sut)
                .as("Sorted by confidence score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);
        
        assertThat(sut.stream())
                .as("Sorted by confidence score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);
        
        assertThat(sut.iterator())
                .as("Sorted by confidence score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);
    }
    
    @Test
    public void thatTopDeltasAreCorrect()
    {
        AnnotationObject rec1Sug1 = new AnnotationObject(1, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "a", "A", "#A", 0.1);
        AnnotationObject rec1Sug2 = new AnnotationObject(2, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "b", "B", "#B", 0.2);
        AnnotationObject rec2Sug1 = new AnnotationObject(3, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "c", "C", "#C", 0.1);
        AnnotationObject rec2Sug2 = new AnnotationObject(4, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "d", "D", "#D", 0.3);

        PredictionGroup sut = new PredictionGroup(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);
                
        // Check that the deltas are ok
        Map<Long, Delta> topDeltas = sut.getTopDeltas();
        assertThat(topDeltas).hasSize(2);
        assertThat(topDeltas.get(1l).getDelta()).isCloseTo(0.1, within(0.00001));
        assertThat(topDeltas.get(2l).getDelta()).isCloseTo(0.2, within(0.00001));
    }
}
