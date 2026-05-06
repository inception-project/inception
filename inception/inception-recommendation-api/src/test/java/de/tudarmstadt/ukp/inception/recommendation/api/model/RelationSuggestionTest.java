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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RelationSuggestionTest
{
    @Test
    void thatCloningRetainsAllInformation()
    {
        var s = RelationSuggestion.builder() //
                .withId(1) //
                .withGeneration(2) //
                .withAge(3) //
                .withRecommenderId(4) //
                .withRecommenderName("rec") //
                .withLayerId(5) //
                .withFeature("feature") //
                .withDocument(1234L) //
                .withLabel("label") //
                .withUiLabel("uiLabel") //
                .withScore(6.0) //
                .withScoreExplanation("scoreExplanation") //
                .withPosition(new RelationPosition(1, 2, 3, 4)) //
                .withAutoAcceptMode(AutoAcceptMode.ON_FIRST_ACCESS) //
                .withHidingFlags(7) //
                .build();

        assertThat(s.toBuilder().build()).usingRecursiveComparison().isEqualTo(s);
    }
}
