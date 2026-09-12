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
package de.tudarmstadt.ukp.inception.editor;

import static de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory.NOT_SUITABLE;
import static de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory.PREFERRED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

class AnnotationEditorRegistryImplTest
{
    private static final String HTML = "html";
    private static final String TEXT = "text";

    private final Project project = new Project("test");

    @Test
    void thatConfiguredEditorIsUsedWhenSuitable()
    {
        var configured = factory("configured", HTML, PREFERRED);
        var other = factory("other", TEXT, PREFERRED);
        var sut = registry(configured, other);

        assertThat(sut.getEditorFactory(project, HTML, "configured")) //
                .as("suitable configured editor wins") //
                .isSameAs(configured);
    }

    @Test
    void thatUnsuitableConfiguredEditorFallsBackToFormatPreferred()
    {
        // "configured" declares itself unable to show HTML, "htmlCapable" prefers it.
        var configured = factory("configured", TEXT, PREFERRED);
        var htmlCapable = factory("htmlCapable", HTML, PREFERRED);
        var sut = registry(configured, htmlCapable);

        assertThat(sut.getEditorFactory(project, HTML, "configured")) //
                .as("unsuitable configured editor is skipped in favor of the format-preferred one") //
                .isSameAs(htmlCapable);
    }

    @Test
    void thatUnknownConfiguredIdFallsBackToFormatPreferred()
    {
        var first = factory("first", TEXT, PREFERRED);
        var htmlCapable = factory("htmlCapable", HTML, PREFERRED);
        var sut = registry(first, htmlCapable);

        assertThat(sut.getEditorFactory(project, HTML, "no-such-editor")) //
                .as("an unknown bean name is treated like no configuration at all") //
                .isSameAs(htmlCapable);
        assertThat(sut.getEditorFactory(project, HTML, null)) //
                .as("no configured editor behaves the same way") //
                .isSameAs(htmlCapable);
    }

    @Test
    void thatDefaultIsUsedWhenNoEditorAcceptsTheFormat()
    {
        var first = factory("first", TEXT, PREFERRED);
        var second = factory("second", TEXT, PREFERRED);
        var sut = registry(first, second);

        assertThat(sut.getEditorFactory(project, "some-exotic-format", "first")) //
                .as("falls back to the default (first registered) editor") //
                .isSameAs(first);
    }

    @Test
    void thatSuitabilityIsReportedForBadgeRendering()
    {
        var textOnly = factory("textOnly", TEXT, PREFERRED);
        var sut = registry(textOnly);

        assertThat(sut.isSuitable(textOnly, project, TEXT)).isTrue();
        assertThat(sut.isSuitable(textOnly, project, HTML)).isFalse();
        assertThat(sut.isSuitable(null, project, TEXT)) //
                .as("a missing factory is never suitable") //
                .isFalse();
    }

    private AnnotationEditorRegistryImpl registry(AnnotationEditorFactory... aFactories)
    {
        var sut = new AnnotationEditorRegistryImpl(asList(aFactories));
        sut.init();
        return sut;
    }

    private static AnnotationEditorFactory factory(String aBeanName, String aAcceptedFormat,
            int aScore)
    {
        return new AnnotationEditorFactory()
        {
            @Override
            public String getBeanName()
            {
                return aBeanName;
            }

            @Override
            public String getDisplayName()
            {
                return aBeanName;
            }

            @Override
            public int accepts(Project aProject, String aFormat)
            {
                return aAcceptedFormat.equals(aFormat) ? aScore : NOT_SUITABLE;
            }

            @Override
            public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel,
                    DocumentEditorManager aManager, AnnotationActionHandler aActionHandler,
                    CasProvider aCasProvider)
            {
                throw new UnsupportedOperationException("Not needed for resolution tests");
            }

            @Override
            public void initState(AnnotatorState aState)
            {
                // Nothing to do
            }

            @Override
            public Optional<PolicyCollection> getPolicy() throws IOException
            {
                return Optional.empty();
            }

            @Override
            public String toString()
            {
                return aBeanName;
            }
        };
    }
}
