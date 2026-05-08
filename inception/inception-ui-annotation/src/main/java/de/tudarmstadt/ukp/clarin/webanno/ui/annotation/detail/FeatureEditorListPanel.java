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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.handleException;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Optional.empty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.util.CachingReuseStrategy;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior;

public class FeatureEditorListPanel
    extends Panel
{
    private static final long serialVersionUID = 908046548492420524L;

    private static final Logger LOG = LoggerFactory.getLogger(FeatureEditorListPanel.class);

    public static final String ID_PREFIX = "featureEditorHead";

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;

    private final WebMarkupContainer featureEditorContainer;
    private final WebMarkupContainer noFeaturesWarning;
    private final FeatureEditorPanelContent featureEditorPanelContent;
    private final AnnotationDetailEditorPanel owner;

    public FeatureEditorListPanel(String aId, IModel<AnnotatorState> aModel,
            AnnotationDetailEditorPanel aOwner)
    {
        super(aId, aModel);

        owner = aOwner;

        featureEditorContainer = new WebMarkupContainer("featureEditorContainer");
        featureEditorContainer.setOutputMarkupPlaceholderTag(true);
        featureEditorContainer
                .add(featureEditorPanelContent = new FeatureEditorPanelContent("featureEditors"));
        featureEditorContainer.add(createFocusResetHelper());
        featureEditorContainer.add(visibleWhen(() -> layerIsSelectedAndHasFeatures()));
        add(featureEditorContainer);

        noFeaturesWarning = new WebMarkupContainer("noFeaturesWarning");
        noFeaturesWarning.setOutputMarkupPlaceholderTag(true);
        noFeaturesWarning.add(visibleWhen(() -> layerIsSelectedButHasNoFeatures()));
        add(noFeaturesWarning);
    }

    private boolean layerIsSelectedAndHasFeatures()
    {
        return getModelObject().getSelection().getAnnotation().isSet()
                && !getModelObject().getFeatureStates().isEmpty();
    }

    private boolean layerIsSelectedButHasNoFeatures()
    {
        return getModelObject().getSelection().getAnnotation().isSet()
                && getModelObject().getFeatureStates().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    private TextField<String> createFocusResetHelper()
    {
        var textfield = new TextField<String>("focusResetHelper");
        textfield.setModel(Model.of());
        textfield.setOutputMarkupId(true);
        textfield.add(new AjaxFormComponentUpdatingBehavior("focus")
        {
            private static final long serialVersionUID = -3030093250599939537L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                var editors = new ArrayList<FeatureEditor>();
                featureEditorPanelContent.getItems().next().visitChildren(FeatureEditor.class,
                        (e, visit) -> {
                            editors.add((FeatureEditor) e);
                            visit.dontGoDeeper();
                        });

                if (!editors.isEmpty()) {
                    aTarget.focusComponent(editors.get(editors.size() - 1).getFocusComponent());
                }
            }
        });

        return textfield;
    }

    public Optional<FeatureEditor> getFirstFeatureEditor()
    {
        var itemIterator = featureEditorPanelContent.getItems();
        if (!itemIterator.hasNext()) {
            return empty();
        }

        return Optional.ofNullable((FeatureEditor) itemIterator.next().get("editor"));
    }

    @OnEvent(stop = true)
    public void onFeatureUpdatedEvent(FeatureEditorValueChangedEvent aEvent)
    {
        var target = aEvent.getTarget();
        try {
            actionFeatureUpdate(aEvent.getEditor(), target);
        }
        catch (Exception e) {
            handleException(this, target, e);
        }
    }

    private static final class IsSidebarAction
        extends MetaDataKey<Boolean>
    {
        private static final long serialVersionUID = 1L;

        public final static IsSidebarAction INSTANCE = new IsSidebarAction();
    }

    private class FeatureEditorPanelContent
        extends RefreshingView<FeatureState>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        FeatureEditorPanelContent(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
            // This strategy caches items as long as the panel exists. This is important to
            // allow the Kendo ComboBox datasources to be re-read when constraints change the
            // available tags.
            setItemReuseStrategy(new CachingReuseStrategy());
        }

        @Override
        protected void onAfterRender()
        {
            super.onAfterRender();

            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                // If the user selects or creates an annotation then we put the focus on the
                // first of the feature editors
                if (!Objects.equals(getRequestCycle().getMetaData(IsSidebarAction.INSTANCE),
                        true)) {
                    getFirstFeatureEditor()
                            .ifPresent(_editor -> autoFocus(_target, _editor.getFocusComponent()));
                }
            });
        }

        @Override
        protected void populateItem(final Item<FeatureState> aItem)
        {
            LOG.trace("FeatureEditorPanelContent.populateItem("
                    + aItem.getModelObject().feature.getUiName() + ": "
                    + aItem.getModelObject().value + ")");

            final var featureState = aItem.getModelObject();

            // Look up a suitable editor and instantiate it
            var featureSupport = featureSupportRegistry.findExtension(featureState.feature)
                    .orElseThrow();
            var editorPanel = findParent(AnnotationDetailEditorPanel.class);
            final var editor = featureSupport.createEditor("editor", featureEditorContainer,
                    editorPanel, getModel(), aItem.getModel());

            // We need to enable the markup ID here because we use it during the AJAX behavior
            // that automatically saves feature editors on change/blur.
            editor.setOutputMarkupPlaceholderTag(true);

            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshes of the feature editor panel. This is required to restore the focus.
            editor.getFocusComponent().setOutputMarkupId(true);
            editor.getFocusComponent()
                    .setMarkupId(ID_PREFIX + editor.getModelObject().feature.getId());
            // Auto-focus is disabled if there are feature editors with key bindings.
            // In that case, at least pressing tab should move the focus to the first
            // editor.
            editor.getFocusComponent()
                    .add(AttributeAppender.replace("tabindex", aItem.getIndex() + 1));

            if (!featureState.feature.getLayer().isReadonly()) {
                var state = getModelObject();

                // Whenever it is updating an annotation, it updates automatically when a
                // component for the feature lost focus - but updating is for every component
                // edited LinkFeatureEditors must be excluded because the auto-update will break
                // the ability to add slots. Adding a slot is NOT an annotation action.
                if (state.getSelection().getAnnotation().isSet()
                        && !(editor instanceof LinkFeatureEditor)) {
                    editor.addFeatureUpdateBehavior();
                }
                else if (!(editor instanceof LinkFeatureEditor)) {
                    editor.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
                    {
                        private static final long serialVersionUID = 5179816588460867471L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget aTarget)
                        {
                            owner.refresh(aTarget);
                        }
                    });
                }

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                Component labelComponent = editor.getLabelComponent();
                // labelComponent.setMarkupId(
                // ID_PREFIX + editor.getModelObject().feature.getId() + "-w-lbl");
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                        featureState.feature.getDescription()));
            }
            else {
                editor.getFocusComponent().setEnabled(false);
            }

            aItem.add(editor);
        }

        @Override
        protected Iterator<IModel<FeatureState>> getItemModels()
        {
            List<FeatureState> featureStates = getModelObject().getFeatureStates();

            return new ModelIteratorAdapter<FeatureState>(featureStates)
            {
                @Override
                protected IModel<FeatureState> model(FeatureState aObject)
                {
                    return FeatureStateModel.of(getModel(), aObject);
                }
            };
        }
    }

    private void actionFeatureUpdate(Component aComponent, AjaxRequestTarget aTarget)
        throws AnnotationException, IOException
    {
        findParent(AnnotationPageBase.class).ensureIsEditable();

        var state = getModelObject();

        if (state.getConstraints() != null) {
            // Make sure we update the feature editor panel because due to
            // constraints the contents may have to be re-rendered
            aTarget.add(featureEditorContainer);
        }

        // When updating an annotation in the sidebar, we must not force a
        // re-focus after rendering
        getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);

        var editorPanel = findParent(AnnotationDetailEditorPanel.class);
        var cas = editorPanel.getEditorCas();

        var adapter = annotationService.getAdapter(state.getSelectedAnnotationLayer());
        editorPanel.commitFeatureStates(aTarget, state.getDocument(), state.getUser().getUsername(),
                cas, state.getSelection().getAnnotation().getId(), adapter,
                state.getFeatureStates());
        editorPanel.internalCompleteAnnotation(aTarget, cas);
        state.clearArmedSlot();

        // If the focus was lost during the update, then try force-focusing the
        // next editor or the first one if we are on the last one.
        if (aTarget.getLastFocusedElementId() == null) {
            var allEditors = new ArrayList<FeatureEditor>();
            visitChildren(FeatureEditor.class, (editor, visit) -> {
                allEditors.add((FeatureEditor) editor);
                visit.dontGoDeeper();
            });

            if (!allEditors.isEmpty()) {
                var currentEditor = aComponent instanceof FeatureEditor //
                        ? (FeatureEditor) aComponent
                        : aComponent.findParent(FeatureEditor.class);

                int i = allEditors.indexOf(currentEditor);

                // If the current editor cannot be found then move the focus to the
                // first editor
                if (i == -1) {
                    autoFocus(aTarget, allEditors.get(0).getFocusComponent());
                }
                // ... if it is the last one, say at the last one
                else if (i >= (allEditors.size() - 1)) {
                    autoFocus(aTarget, allEditors.get(allEditors.size() - 1).getFocusComponent());
                }
                // ... otherwise move the focus to the next editor
                else {
                    autoFocus(aTarget, allEditors.get(i + 1).getFocusComponent());
                }
            }
        }
    }

    public void autoFocus(AjaxRequestTarget aTarget, Component aComponent)
    {
        // Check if any of the features suppresses auto-focus...
        for (FeatureState fstate : getModelObject().getFeatureStates()) {
            AnnotationFeature feature = fstate.getFeature();
            FeatureSupport<?> fs = featureSupportRegistry.findExtension(feature).orElseThrow();
            if (fs.suppressAutoFocus(feature)) {
                return;
            }
        }

        aTarget.focusComponent(aComponent);
    }
}
