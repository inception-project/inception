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
package de.tudarmstadt.ukp.inception.annotation.layer.document.sidebar;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureEditor;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class DocumentMetadataAnnotationDetailPanel
    extends Panel
{
    private static final long serialVersionUID = 2713520228348549734L;

    private static final Logger LOG = LoggerFactory
            .getLogger(DocumentMetadataAnnotationDetailPanel.class);

    private static final String CID_EDITOR = "editor";
    private static final String CID_FEATURE_EDITORS = "featureEditors";

    public static final String ID_PREFIX = "metaFeatureEditorHead";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private final AnnotationPageBase2 annotationPage;
    private final CasProvider jcasProvider;
    private final IModel<Project> project;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<User> user;
    private final ListView<FeatureState> featureList;
    private final AnnotationActionHandler actionHandler;
    private final IModel<AnnotatorState> state;

    public DocumentMetadataAnnotationDetailPanel(String aId, IModel<VID> aModel,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage,
            AnnotationActionHandler aActionHandler, IModel<AnnotatorState> aState)
    {
        super(aId, aModel);

        setOutputMarkupPlaceholderTag(true);

        sourceDocument = aState.map(AnnotatorState::getDocument);
        user = aState.map(AnnotatorState::getUser);
        annotationPage = aAnnotationPage;
        jcasProvider = aCasProvider;
        project = aState.map(AnnotatorState::getProject);
        actionHandler = aActionHandler;
        state = aState;

        add(featureList = createFeaturesList());
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        add(visibleWhen(this::isVisible));
        setEnabled(annotationPage.isEditable()
                && !getLayer().map(AnnotationLayer::isReadonly).orElse(true));
    }

    public VID getModelObject()
    {
        return (VID) getDefaultModelObject();
    }

    private ListView<FeatureState> createFeaturesList()
    {
        return new ListView<FeatureState>(CID_FEATURE_EDITORS,
                LoadableDetachableModel.of(this::listFeatures))
        {
            private static final long serialVersionUID = -1139622234318691941L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                final FeatureState featureState = item.getModelObject();
                final FeatureEditor editor;

                // Look up a suitable editor and instantiate it
                var featureSupport = featureSupportRegistry.findExtension(featureState.feature)
                        .orElseThrow();
                editor = featureSupport.createEditor(CID_EDITOR,
                        DocumentMetadataAnnotationDetailPanel.this, actionHandler,
                        annotationPage.getModel(), item.getModel());

                if (!featureState.feature.getLayer().isReadonly()) {
                    // Whenever it is updating an annotation, it updates automatically when a
                    // component for the feature lost focus - but updating is for every component
                    // edited LinkFeatureEditors must be excluded because the auto-update will break
                    // the ability to add slots. Adding a slot is NOT an annotation action.
                    if (!(editor instanceof LinkFeatureEditor)) {
                        editor.addFeatureUpdateBehavior();
                    }

                    // Add tooltip on label
                    var tooltipTitle = new StringBuilder();
                    tooltipTitle.append(featureState.feature.getUiName());
                    if (featureState.feature.getTagset() != null) {
                        tooltipTitle.append(" (");
                        tooltipTitle.append(featureState.feature.getTagset().getName());
                        tooltipTitle.append(')');
                    }

                    var labelComponent = editor.getLabelComponent();
                    labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                    labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                            featureState.feature.getDescription()));
                }
                else {
                    editor.getFocusComponent().setEnabled(false);
                }

                // We need to enable the markup ID here because we use it during the AJAX behavior
                // that automatically saves feature editors on change/blur.
                // Check addAnnotateActionBehavior.
                editor.setOutputMarkupPlaceholderTag(true);

                // Ensure that markup IDs of feature editor focus components remain constant
                // across refreshes of the feature editor panel. This is required to restore the
                // focus.
                editor.getFocusComponent().setOutputMarkupId(true);
                editor.getFocusComponent()
                        .setMarkupId(ID_PREFIX + editor.getModelObject().feature.getId());

                item.add(editor);
            }
        };
    }

    private Optional<AnnotationLayer> getLayer()
    {
        var vid = getModelObject();
        var proj = project.getObject();
        if (proj == null || vid == null || vid.isNotSet() || vid.isSynthetic()) {
            return Optional.empty();
        }

        CAS cas;
        try {
            cas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return Optional.empty();
        }

        FeatureStructure fs;
        try {
            fs = ICasUtil.selectFsByAddr(cas, vid.getId());
        }
        catch (Exception e) {
            LOG.error("Unable to locate annotation with ID {}", vid);
            return Optional.empty();
        }

        return Optional.of(annotationService.findLayer(proj, fs));
    }

    private List<FeatureState> listFeatures()
    {
        var vid = getModelObject();
        var proj = project.getObject();

        if (proj == null || vid == null || vid.isNotSet() || vid.isSynthetic()) {
            return emptyList();
        }

        CAS cas;
        try {
            cas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        FeatureStructure fs;
        try {
            fs = ICasUtil.selectFsByAddr(cas, vid.getId());
        }
        catch (Exception e) {
            LOG.error("Unable to locate annotation with ID {}", vid);
            return emptyList();
        }

        var layer = annotationService.findLayer(proj, fs);
        var adapter = annotationService.getAdapter(layer);

        // Populate from feature structure
        var featureStates = new ArrayList<FeatureState>();
        for (var feature : annotationService.listEnabledFeatures(layer)) {
            Serializable value = null;
            if (fs != null) {
                value = adapter.getFeatureValue(feature, fs);
            }

            var featureState = new FeatureState(vid, feature, value);
            featureStates.add(featureState);
            featureState.tagset = annotationService
                    .listTagsReorderable(featureState.feature.getTagset());
        }

        return featureStates;
    }

    private void actionAnnotate(AjaxRequestTarget aTarget)
    {
        try {
            annotationPage.ensureIsEditable();

            // When updating an annotation in the sidebar, we must not force a
            // re-focus after rendering
            getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);

            // Load the boiler-plate
            CAS cas = jcasProvider.get();
            FeatureStructure fs = ICasUtil.selectFsByAddr(cas, getModelObject().getId());
            AnnotationLayer layer = annotationService.findLayer(project.getObject(), fs);
            TypeAdapter adapter = annotationService.getAdapter(layer);

            // Update the features of the selected annotation from the values presently in
            // the feature editors
            writeFeatureEditorModelsToCas(adapter, cas);

            // persist changes
            annotationPage.writeEditorCas(cas);

            findParent(AnnotationPageBase.class).actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            WicketExceptionUtil.handleException(LOG, DocumentMetadataAnnotationDetailPanel.this,
                    aTarget, e);
        }
    }

    private void writeFeatureEditorModelsToCas(TypeAdapter aAdapter, CAS aCas)
        throws IOException, AnnotationException
    {
        List<FeatureState> featureStates = featureList.getModelObject();

        LOG.trace("writeFeatureEditorModelsToCas()");
        List<AnnotationFeature> features = new ArrayList<>();
        for (FeatureState featureState : featureStates) {
            features.add(featureState.feature);

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(featureState.feature.getType())) {
                String value = (String) featureState.value;

                if (value != null && featureState.feature.getTagset() != null
                        && featureState.feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, featureState.feature.getTagset())) {
                    Tag selectedTag = new Tag();
                    selectedTag.setName(value);
                    selectedTag.setTagSet(featureState.feature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }

            LOG.trace("writeFeatureEditorModelsToCas() " + featureState.feature.getUiName() + " = "
                    + featureState.value);
            aAdapter.setFeatureValue(sourceDocument.getObject(), user.getObject().getUsername(),
                    aCas, getModelObject().getId(), featureState.feature, featureState.value);
        }
    }

    private static final class IsSidebarAction
        extends MetaDataKey<Boolean>
    {
        private static final long serialVersionUID = 1L;

        public final static IsSidebarAction INSTANCE = new IsSidebarAction();
    }

    public void toggleVisibility()
    {
        state.getObject().clearArmedSlot();
        setVisible(!isVisible());
    }

    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        try {
            CAS cas = jcasProvider.get();
            AnnotationFS fs = ICasUtil.selectAnnotationByAddr(cas,
                    aEvent.getLinkWithRoleModel().targetAddr);
            state.getObject().getSelection().selectSpan(fs);
            if (state.getObject().getSelection().getAnnotation().isSet()) {
                actionHandler.actionDelete(target);

                findParent(AnnotationPageBase.class).actionRefreshDocument(aEvent.getTarget());
            }
        }
        catch (IOException | AnnotationException e) {
            WicketExceptionUtil.handleException(LOG, DocumentMetadataAnnotationDetailPanel.this,
                    target, e);
        }
    }

    @OnEvent(stop = true)
    public void onFeatureUpdatedEvent(FeatureEditorValueChangedEvent aEvent)
    {
        actionAnnotate(aEvent.getTarget());

        findParent(AnnotationPageBase.class).actionRefreshDocument(aEvent.getTarget());
    }
}
