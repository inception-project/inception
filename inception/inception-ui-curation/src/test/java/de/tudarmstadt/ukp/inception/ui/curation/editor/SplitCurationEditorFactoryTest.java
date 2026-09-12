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
package de.tudarmstadt.ukp.inception.ui.curation.editor;

import static de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory.NOT_SUITABLE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;

class SplitCurationEditorFactoryTest
{
    private SplitCurationEditorFactory sut;

    @BeforeEach
    void setup()
    {
        sut = new SplitCurationEditorFactory();
    }

    /**
     * The host creates the editor and only then calls {@code initState}. Since the inner text
     * editor establishes the paging strategy while it is being created, anything this factory sets
     * afterwards would silently replace it - which is why it has to leave the state alone.
     */
    @Test
    void thatInitStateLeavesThePagingStrategyOfTheInnerEditorAlone()
    {
        var state = new AnnotatorStateImpl(null);
        var innerStrategy = new LineOrientedPagingStrategy();
        state.setPagingStrategy(innerStrategy);

        sut.initState(state);

        assertThat(state.getPagingStrategy()) //
                .as("the inner editor's paging strategy survives initState") //
                .isSameAs(innerStrategy);
    }

    /**
     * The split view is only ever shown because the split-curation page pins it, so it must never
     * win an ordinary editor resolution.
     */
    @Test
    void thatItNeverOffersItselfAsAnOrdinaryEditor()
    {
        assertThat(sut.accepts(null, "text")).isEqualTo(NOT_SUITABLE);
    }

    /**
     * {@code NOT_SUITABLE} only affects resolution - the settings dropdown lists factories without
     * consulting it, so hiding it there takes this separate flag.
     */
    @Test
    void thatItIsNotOfferedInTheEditorSettings()
    {
        assertThat(sut.isUserSelectable()).isFalse();
    }
}
