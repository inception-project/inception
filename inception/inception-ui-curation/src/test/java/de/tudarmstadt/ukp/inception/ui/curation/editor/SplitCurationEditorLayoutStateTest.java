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

import static de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorLayoutState.EDITOR_SIZE_DEFAULT;
import static de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorLayoutState.EDITOR_SIZE_MAX;
import static de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorLayoutState.EDITOR_SIZE_MIN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SplitCurationEditorLayoutStateTest
{
    @Test
    void thatTheEditorSizeDefaultsWhenNeverSet()
    {
        assertThat(new SplitCurationEditorLayoutState().getEditorSize())
                .isEqualTo(EDITOR_SIZE_DEFAULT);
    }

    @Test
    void thatAnEditorSizeInRangeIsKept()
    {
        var sut = new SplitCurationEditorLayoutState();
        sut.setEditorSize(72.5);
        assertThat(sut.getEditorSize()).isEqualTo(72.5);
    }

    /**
     * Dragging the splitter to an extreme reports a size slightly past the bound because of
     * splitbar-width rounding. Those have to be clamped and stored, not rejected - rejecting them
     * would make the pane snap back to its previous size on the next page load.
     */
    @Test
    void thatOutOfRangeEditorSizesAreClampedRatherThanDropped()
    {
        var sut = new SplitCurationEditorLayoutState();

        sut.setEditorSize(EDITOR_SIZE_MAX + 3.0);
        assertThat(sut.getEditorSize()).isEqualTo(EDITOR_SIZE_MAX);

        sut.setEditorSize(EDITOR_SIZE_MIN - 3.0);
        assertThat(sut.getEditorSize()).isEqualTo(EDITOR_SIZE_MIN);
    }

    /**
     * This is a two-pane vertical splitter, so it must not inherit the annotation page's sidebar
     * cap of roughly a third - that exists only because two sidebars plus a center area have to
     * fit.
     */
    @Test
    void thatTheEditorMayTakeMostOfTheHeight()
    {
        var sut = new SplitCurationEditorLayoutState();
        sut.setEditorSize(80.0);
        assertThat(sut.getEditorSize()).isEqualTo(80.0);
    }
}
