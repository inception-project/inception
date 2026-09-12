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
import static de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorLayoutState.KEY_SPLIT_CURATION_EDITOR_LAYOUT_STATE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.LinkedList;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.wicketstuff.jquery.core.Options;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratLineOrientedAnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratSentenceOrientedAnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.AnnotatorsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotatorSegmentState;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorContentReplacedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.kendo.AjaxSplitterBehavior;

/**
 * Editor resembling the split pane approach of the legacy curation page.
 */
public class SplitCurationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -6528200507315665141L;

    private static final String MID_SPLITTER = "splitter";
    private static final String MID_EDITOR = "editor";
    private static final String MID_ANNOTATORS_PANEL = "annotatorsPanel";

    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean UserDao userRepository;

    private final WebMarkupContainer splitter;
    private final AjaxSplitterBehavior splitterBehavior;
    private final AnnotationEditorBase innerEditor;
    private final AnnotatorsPanel annotatorsPanel;

    public SplitCurationEditor(String aId, IModel<AnnotatorState> aModel,
            DocumentEditorManager aManager, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider)
    {
        super(aId, aModel, aManager, aActionHandler, aCasProvider);

        splitter = new WebMarkupContainer(MID_SPLITTER);
        splitter.setOutputMarkupId(true);
        splitterBehavior = createSplitterBehavior();
        splitter.add(splitterBehavior);
        add(splitter);

        innerEditor = createInnerEditor(MID_EDITOR);
        splitter.add(innerEditor);

        var segments = new LinkedList<AnnotatorSegmentState>();
        var segment = new AnnotatorSegmentState();
        segment.setAnnotatorState(getModelObject());
        segments.add(segment);

        annotatorsPanel = new AnnotatorsPanel(MID_ANNOTATORS_PANEL, aManager,
                new ListModel<>(segments));
        annotatorsPanel.setOutputMarkupPlaceholderTag(true);
        annotatorsPanel.add(visibleWhen(getModel().map(AnnotatorState::getDocument).isPresent()));
        splitter.add(annotatorsPanel);
    }

    private AjaxSplitterBehavior createSplitterBehavior()
    {
        return new AjaxSplitterBehavior("#" + splitter.getMarkupId(),
                AjaxSplitterBehavior.Orientation.VERTICAL, new Options())
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(Component aComponent, IHeaderResponse aResponse)
            {
                // This splitter is delivered by an Ajax response into a container that is still
                // being laid out, so the client must not try to infer the sizes from the widget -
                // declaring the panes here also declares the percentages to restore.
                setPanes(new Options("size", Options.asString(loadEditorSize() + "%")) //
                        .set("min", Options.asString(EDITOR_SIZE_MIN + "%")) //
                        .set("max", Options.asString(EDITOR_SIZE_MAX + "%")), new Options());
                super.renderHead(aComponent, aResponse);
            }

            @Override
            protected void onResize(AjaxRequestTarget aTarget, double[] aSizes)
            {
                if (aSizes.length >= 1) {
                    persistEditorSize(aSizes[0]);
                }
            }
        };
    }

    private double loadEditorSize()
    {
        var project = getModelObject().getProject();
        if (project == null) {
            return EDITOR_SIZE_DEFAULT;
        }

        return preferencesService
                .loadTraitsForUserAndProject(KEY_SPLIT_CURATION_EDITOR_LAYOUT_STATE,
                        userRepository.getCurrentUser(), project)
                .getEditorSize();
    }

    private void persistEditorSize(double aSize)
    {
        var project = getModelObject().getProject();
        if (project == null) {
            return;
        }

        var sessionOwner = userRepository.getCurrentUser();
        var layoutState = preferencesService.loadTraitsForUserAndProject(
                KEY_SPLIT_CURATION_EDITOR_LAYOUT_STATE, sessionOwner, project);
        layoutState.setEditorSize(aSize);
        preferencesService.saveTraitsForUserAndProject(KEY_SPLIT_CURATION_EDITOR_LAYOUT_STATE,
                sessionOwner, project, layoutState);
    }

    private AnnotationEditorBase createInnerEditor(String aId)
    {
        var state = getModelObject();

        var editorId = annotationService.isSentenceLayerEditable(state.getProject())
                ? BratLineOrientedAnnotationEditorFactory.ID
                : BratSentenceOrientedAnnotationEditorFactory.ID;

        var factory = editorRegistry.getEditorFactory(editorId);
        if (factory == null) {
            if (state.getDocument() != null) {
                factory = editorRegistry.getPreferredEditorFactory(state.getProject(),
                        state.getDocument().getFormat());
            }
            else {
                factory = editorRegistry.getDefaultEditorFactory();
            }
        }

        state.setEditorFactoryId(factory.getBeanName());

        var editor = factory.create(aId, getModel(), getDocumentEditorManager(), getActionHandler(),
                getCasProvider());
        editor.add(visibleWhen(getModel().map(AnnotatorState::getDocument).isPresent()));
        editor.setOutputMarkupPlaceholderTag(true);

        // Establish the paging strategy of the editor that actually shows the text. The outer
        // factory's initState deliberately does nothing, so this is the only place it is set.
        factory.initState(state);

        return editor;
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        // We do not use @OnEvent here because it skips invisible components

        super.onEvent(aEvent);

        if (!(aEvent.getPayload() instanceof EditorContentReplacedEvent event)) {
            return;
        }

        // The event is broadcast across the whole page, so ignore the ones belonging to another
        // editor's state (e.g. a reference-document sidebar viewer).
        if (event.getSource() != getModelObject()) {
            return;
        }

        try {
            annotatorsPanel.init(event.getRequestHandler(), getModelObject());
        }
        catch (Exception e) {
            handleError("Unable to load the annotator's documents", e);
        }
    }

    @Override
    public void requestRender(AjaxRequestTarget aTarget)
    {
        if (aTarget == null) {
            return;
        }

        innerEditor.requestRender(aTarget);
        annotatorsPanel.requestRender(aTarget, getModelObject());
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // Nothing to do - requestRender delegates to the two halves, which render themselves.
    }

    @Override
    protected void onRemove()
    {
        getRequestCycle().find(AjaxRequestTarget.class).ifPresent(splitterBehavior::destroy);

        super.onRemove();
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        // The curator's editor is where annotations are edited, so its context menu is the one the
        // editor actions expect to open. The annotators panel drives its own context menu directly
        // and does not go through this lookup.
        return innerEditor.getContextMenuLookup();
    }
}
