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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSetMarker.FORMER_ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSetMarker.MISSING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

class AnnotationSetTest
{
    @Test
    void thatCurrentAnnotatorIsNotFlaggedAsFormer()
    {
        var user = User.builder().withUsername("bob").withUiName("Bob").build();

        var set = AnnotationSet.forUser(user);

        assertThat(set.id()).isEqualTo("bob");
        assertThat(set.name()).isEqualTo("Bob");
        assertThat(set.displayName()).isEqualTo("Bob");
        assertThat(set.marker()).isNull();
        assertThat(set.hasAnyMarkers(FORMER_ANNOTATOR, MISSING)).isFalse();
    }

    @Test
    void thatFormerAnnotatorIsFlaggedAndDisplayNameIsMarked()
    {
        var user = User.builder().withUsername("carol").withUiName("Carol").build();

        var set = AnnotationSet.forUser(user, FORMER_ANNOTATOR);

        assertThat(set.id()).isEqualTo("carol");
        assertThat(set.name()).isEqualTo("Carol");
        assertThat(set.displayName()).isEqualTo("Carol (former annotator)");
        assertThat(set.marker()).isEqualTo(FORMER_ANNOTATOR);
        assertThat(set.hasAnyMarkers(FORMER_ANNOTATOR, MISSING)).isTrue();
        assertThat(set.hasAnyMarkers(MISSING)).isFalse();
    }

    @Test
    void thatMissingAnnotatorIsFlaggedAsFormer()
    {
        var set = AnnotationSet.forUser("ghost", MISSING);

        assertThat(set.id()).isEqualTo("ghost");
        assertThat(set.name()).isEqualTo("ghost");
        assertThat(set.displayName()).isEqualTo("ghost (missing!)");
        assertThat(set.marker()).isEqualTo(MISSING);
        assertThat(set.hasAnyMarkers(FORMER_ANNOTATOR, MISSING)).isTrue();
    }

    @Test
    void thatEqualityIsByIdOnlyRegardlessOfDisplayNameAndMarker()
    {
        // The matrix relies on this: a marked column set must still match the unmarked set under
        // which the annotation document is stored in the cell map.
        var marked = AnnotationSet.forUser("carol", FORMER_ANNOTATOR);
        var plain = AnnotationSet.forUser("carol");

        assertThat(marked).isEqualTo(plain);
        assertThat(marked.hashCode()).isEqualTo(plain.hashCode());
    }
}
