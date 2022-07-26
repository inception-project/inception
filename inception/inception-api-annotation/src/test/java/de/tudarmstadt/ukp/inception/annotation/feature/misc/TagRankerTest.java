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
package de.tudarmstadt.ukp.inception.annotation.feature.misc;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;

public class TagRankerTest
{
    private TagRanker sut;

    @BeforeEach
    public void setup()
    {
        sut = new TagRanker();
        sut.setMaxResults(10);
        sut.setTagCreationAllowed(false);
    }

    @Test
    public void thatReorderedTagsComeFirst()
    {
        List<ReorderableTag> tagset = asList( //
                new ReorderableTag("normal", false), //
                new ReorderableTag("reordered", true));

        List<ReorderableTag> result = sut.rank("", tagset);

        assertThat(result) //
                .extracting(ReorderableTag::getName) //
                .containsExactly("reordered", "normal");
    }

    @Test
    public void thatNewTagIsNotOfferedWhenItDoesNotExistYet()
    {
        sut.setTagCreationAllowed(false);

        List<ReorderableTag> result = sut.rank("test", emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    public void thatNewTagIsOfferedWhenItDoesNotExistYet()
    {
        sut.setTagCreationAllowed(true);

        List<ReorderableTag> result = sut.rank("new", emptyList());

        assertThat(result) //
                .extracting(ReorderableTag::getName) //
                .containsExactly("new");
    }

    @Test
    public void thatNewTagOfferedBeforeReorderedTags()
    {
        sut.setTagCreationAllowed(true);

        List<ReorderableTag> tagset = asList( //
                new ReorderableTag("normal", false), //
                new ReorderableTag("reordered", true));

        List<ReorderableTag> result = sut.rank("nor", tagset);

        assertThat(result) //
                .extracting(ReorderableTag::getName) //
                .containsExactly("nor", "reordered", "normal");
    }

    @Test
    public void thatExistingTagIsOfferedFirstWhenCreationIsAllowed()
    {
        sut.setTagCreationAllowed(true);

        List<ReorderableTag> tagset = asList( //
                new ReorderableTag("normal", false), //
                new ReorderableTag("reordered", true));

        List<ReorderableTag> result = sut.rank("normal", tagset);

        assertThat(result) //
                .extracting(ReorderableTag::getName, ReorderableTag::getDescription) //
                .containsExactly( //
                        tuple("normal", null), //
                        tuple("reordered", null));
    }
}
