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

import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class AnnotationObjectTest
{
    @Test
    public void thatEqualsAndHashCodeAndCompareToWorkCorrectly()
    {
        AnnotationObject rec1Sug1 = new AnnotationObject(1, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "a", "A", "#A", 0.1);
        AnnotationObject rec1Sug2 = new AnnotationObject(2, 1, "rec1", "value", "doc1", "doc1Uri",
                0, 1, "b", "B", "#B", 0.2);
        AnnotationObject rec2Sug1 = new AnnotationObject(3, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "c", "C", "#C", 0.1);
        AnnotationObject rec2Sug2 = new AnnotationObject(4, 2, "rec2", "value", "doc1", "doc1Uri",
                0, 1, "d", "D", "#D", 0.3);

        AnnotationObject[] all = new AnnotationObject[] {rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2};
        for (AnnotationObject x : all) {
            for (AnnotationObject y : all) {
                if (x == y) {
                    assertThat(x).isEqualTo(y);
                    assertThat(x).isEqualByComparingTo(y);
                    assertThat(y).isEqualTo(x);
                    assertThat(y).isEqualByComparingTo(x);
                    assertThat(x.hashCode()).isEqualTo(y.hashCode());
                }
                else {
                    assertThat(x).isNotEqualTo(y);
                    assertThat(x).isNotEqualByComparingTo(y);
                    assertThat(y).isNotEqualTo(x);
                    assertThat(y).isNotEqualByComparingTo(x);
                    assertThat(x.hashCode()).isNotEqualTo(y.hashCode());
                }
            }
        }
    }
}
