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
package de.tudarmstadt.ukp.inception.curation.api;

import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

class Position_ImplBaseTest
{
    @Test
    void thatEqualsDoesNotNpeForNonLinkPositions()
    {
        // Two non-link positions: linkFeatureMultiplicityMode is null for both. The previous
        // implementation fell through to a switch on the null enum and NPE'd via ordinal().
        var a = new TestPosition("coll", "doc", "T");
        var b = new TestPosition("coll", "doc", "T");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isEqualByComparingTo(b);
    }

    @Test
    void thatEqualsReturnsFalseWhenTypeDiffersAndMultiplicityIsNull()
    {
        var a = new TestPosition("coll", "doc", "T1");
        var b = new TestPosition("coll", "doc", "T2");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void thatEqualsStillWorksForLinkPositions()
    {
        var a = new TestPosition("coll", "doc", "T", "feat", "role", 0, 5, "txt",
                ONE_TARGET_MULTIPLE_ROLES);
        var b = new TestPosition("coll", "doc", "T", "feat", "role", 0, 5, "txt",
                ONE_TARGET_MULTIPLE_ROLES);
        var differentRole = new TestPosition("coll", "doc", "T", "feat", "other", 0, 5, "txt",
                ONE_TARGET_MULTIPLE_ROLES);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(differentRole);
    }

    private static final class TestPosition
        extends Position_ImplBase
    {
        private static final long serialVersionUID = 1L;

        TestPosition(String aCollectionId, String aDocumentId, String aType)
        {
            super(aCollectionId, aDocumentId, aType);
        }

        TestPosition(String aCollectionId, String aDocumentId, String aType, String aFeature,
                String aRole, int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
                LinkFeatureMultiplicityMode aBehavior)
        {
            super(aCollectionId, aDocumentId, aType, aFeature, aRole, aLinkTargetBegin,
                    aLinkTargetEnd, aLinkTargetText, aBehavior);
        }

        @Override
        public String toMinimalString()
        {
            return toString();
        }
    }
}
