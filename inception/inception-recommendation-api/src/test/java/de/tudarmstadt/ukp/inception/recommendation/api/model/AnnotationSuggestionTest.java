/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AnnotationSuggestionTest
{
    @Test
    public void thatEqualsAndHashCodeAndCompareToWorkCorrectly()
    {
        SpanSuggestion rec1Sug1 = new SpanSuggestion(1, 1, "rec1", 1, "value", "doc1", 0, 1, "a",
                "A", "#A", 0.1, "E1");
        SpanSuggestion rec1Sug2 = new SpanSuggestion(2, 1, "rec1", 1, "value", "doc1", 0, 1, "b",
                "B", "#B", 0.2, "E2");
        SpanSuggestion rec2Sug1 = new SpanSuggestion(3, 2, "rec2", 1, "value", "doc1", 0, 1, "c",
                "C", "#C", 0.1, "E1");
        SpanSuggestion rec2Sug2 = new SpanSuggestion(4, 2, "rec2", 1, "value", "doc1", 0, 1, "d",
                "D", "#D", 0.3, "E3");

        List<SpanSuggestion> all = asList(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);
        for (SpanSuggestion x : all) {
            for (SpanSuggestion y : all) {
                if (x == y) {
                    assertThat(x).isEqualTo(y);
                    assertThat(y).isEqualTo(x);
                    assertThat(x.hashCode()).isEqualTo(y.hashCode());
                }
                else {
                    assertThat(x).isNotEqualTo(y);
                    assertThat(y).isNotEqualTo(x);
                    assertThat(x.hashCode()).isNotEqualTo(y.hashCode());
                }
            }
        }
    }
}
