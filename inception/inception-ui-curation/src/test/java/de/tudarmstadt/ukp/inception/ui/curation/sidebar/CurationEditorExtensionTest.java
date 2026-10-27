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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.curation.api.CurationVID;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ScrollToHandler;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
class CurationEditorExtensionTest
{
    private @Mock AnnotationSchemaService schemaService;
    private @Mock DocumentService documentService;
    private @Mock DiamContext context;
    private @Mock AnnotationActionHandler actionHandler;
    private @Mock AjaxRequestTarget target;

    private CurationEditorExtension sut;

    private Project project;
    private SourceDocument document;
    private AnnotatorStateImpl state;
    private CAS annotatorCas;
    private Annotation suggestion;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new CurationEditorExtension(schemaService, documentService, null, null, null, null,
                null, null, null);

        project = Project.builder().withId(1l).withName("project").build();
        document = SourceDocument.builder().withId(1l).withProject(project).withName("doc").build();

        state = new AnnotatorStateImpl(ANNOTATION);
        state.setProject(project);
        state.setDocument(document, emptyList());
        when(context.getAnnotatorState()).thenReturn(state);

        annotatorCas = CasFactory.createText("This is a test.");
        suggestion = new Annotation(annotatorCas.getJCas(), 5, 7);
        suggestion.addToIndexes();
    }

    @Test
    void thatAnActionOnASuggestionIsHandledWhereTheCurationExtensionIsEnabled() throws Exception
    {
        state.enableExtension(CurationEditorExtension.EXTENSION_ID);
        when(documentService.existsAnnotationDocument(document, AnnotationSet.forUser("anno1")))
                .thenReturn(true);
        when(documentService.readAnnotationCas(document, AnnotationSet.forUser("anno1")))
                .thenReturn(annotatorCas);
        when(schemaService.findLayer(project, suggestion)).thenReturn(AnnotationLayer.builder() //
                .withProject(project) //
                .withName(Annotation.class.getName()) //
                .withType(SpanLayerSupport.TYPE) //
                .build());
        when(context.getActionHandler()).thenReturn(actionHandler);

        sut.handleAction(context, target, suggestionOf("anno1"), ScrollToHandler.COMMAND);

        verify(actionHandler).actionJump(target, 5, 7);
    }

    @Test
    void thatAnActionOnASuggestionIsRefusedWhereTheCurationExtensionIsNotEnabled() throws Exception
    {
        // A client can send a curation VID to any editor, e.g. on the annotation page, to get at
        // another annotator's annotations
        sut.handleAction(context, target, suggestionOf("anno1"), ScrollToHandler.COMMAND);

        verifyNoInteractions(documentService, actionHandler);
    }

    @Test
    void thatALookupOfSuggestionDetailsIsRefusedWhereTheCurationExtensionIsNotEnabled()
        throws Exception
    {
        // A client can send a curation VID to any editor, e.g. on the annotation page, to get at
        // another annotator's annotations
        var layer = AnnotationLayer.builder() //
                .withProject(project) //
                .withName(Annotation.class.getName()) //
                .withType(SpanLayerSupport.TYPE) //
                .build();

        var details = sut.lookupLazyDetails(context, document, null, annotatorCas,
                suggestionOf("anno1"), layer);

        assertThat(details).isEmpty();
        verifyNoInteractions(documentService);
    }

    private VID suggestionOf(String aAnnotator)
    {
        var curationVid = new CurationVID(aAnnotator,
                VID.builder().forAnnotation(suggestion).build());
        // The editor sends the VID in its string form, which handleAction receives parsed
        return VID.parse(curationVid.toString());
    }
}
