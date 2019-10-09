/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;

public class DocumentMetadataAnnotationDetailPanel extends Panel
{
    private static final long serialVersionUID = 2713520228348549734L;

    private static final Logger LOG = LoggerFactory
            .getLogger(DocumentMetadataAnnotationDetailPanel.class);

    private static final String CID_EDITOR = "editor";
    private static final String CID_FEATURE_VALUES = "featureValues";
    private static final String CID_DELETE = "delete";

    public static final String ID_PREFIX = "metaFeatureEditorHead";
    
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private final AnnotationPage annotationPage;
    private final CasProvider jcasProvider;
    private final IModel<Project> project;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<String> username;
    private final ListView<FeatureState> featureList;
    private final DocumentMetadataAnnotationSelectionPanel selectionPanel;
    private final AnnotationActionHandler actionHandler;
    private final AnnotatorState state;
    
    public DocumentMetadataAnnotationDetailPanel(String aId, IModel<VID> aModel,
            IModel<SourceDocument> aDocument, IModel<String> aUsername, CasProvider aCasProvider,
            IModel<Project> aProject, AnnotationPage aAnnotationPage,
            DocumentMetadataAnnotationSelectionPanel aSelectionPanel,
            AnnotationActionHandler aActionHandler, AnnotatorState aState)
    {
        super(aId, aModel);

        setOutputMarkupPlaceholderTag(true);
        
        sourceDocument = aDocument;
        username = aUsername;
        annotationPage = aAnnotationPage;
        jcasProvider = aCasProvider;
        project = aProject;
        selectionPanel = aSelectionPanel;
        actionHandler = aActionHandler;
        state = aState;
        
        add(featureList = createFeaturesList());
        
        add(new LambdaAjaxLink(CID_DELETE, this::actionDelete));
        
        add(LambdaBehavior.visibleWhen(this::isVisible));
    }
    
    public VID getModelObject()
    {
        return (VID) getDefaultModelObject();
    }

    private ListView<FeatureState> createFeaturesList()
    {
        return new ListView<FeatureState>(CID_FEATURE_VALUES,
                LoadableDetachableModel.of(this::listFeatures))
        {
            private static final long serialVersionUID = -1139622234318691941L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                // Feature editors that allow multiple values may want to update themselves,
                // e.g. to add another slot.
                item.setOutputMarkupId(true);

                final FeatureState featureState = item.getModelObject();
                final FeatureEditor editor;
                
                // Look up a suitable editor and instantiate it
                FeatureSupport featureSupport = featureSupportRegistry
                        .getFeatureSupport(featureState.feature);
                editor = featureSupport.createEditor(CID_EDITOR,
                        DocumentMetadataAnnotationDetailPanel.this, actionHandler,
                         annotationPage.getModel(), item.getModel());

                if (!featureState.feature.getLayer().isReadonly()) {
                    // Whenever it is updating an annotation, it updates automatically when a
                    // component for the feature lost focus - but updating is for every component
                    // edited LinkFeatureEditors must be excluded because the auto-update will break
                    // the ability to add slots. Adding a slot is NOT an annotation action.
                    AnnotationFeature feature = featureState.feature;
                    if (!(feature.getMultiValueMode().equals(MultiValueMode.ARRAY)
                        && feature.getLinkMode().equals(LinkMode.WITH_ROLE))) {
                        addAnnotateActionBehavior(editor);
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
                editor.setOutputMarkupId(true);
                editor.setOutputMarkupPlaceholderTag(true);
                
                // Ensure that markup IDs of feature editor focus components remain constant
                // across refreshes of the feature editor panel. This is required to restore the
                // focus.
                editor.getFocusComponent().setOutputMarkupId(true);
                editor.getFocusComponent().setMarkupId(
                        ID_PREFIX + editor.getModelObject().feature.getId());
                
                item.add(editor);
            }
        };
    }
    
    private List<FeatureState> listFeatures()
    {
        VID vid = getModelObject();
        Project proj = project.getObject();
        
        if (proj == null || vid == null || vid.isNotSet()) {
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
            fs = selectFsByAddr(cas, vid.getId());
        }
        catch (Exception e) {
            LOG.error("Unable to locate annotation with ID {}", vid);
            return emptyList();
        }
        AnnotationLayer layer = annotationService.findLayer(proj, fs);
        TypeAdapter adapter = annotationService.getAdapter(layer);
        
        // Populate from feature structure
        List<FeatureState> featureStates = new ArrayList<>();
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
            if (!feature.isEnabled()) {
                continue;
            }

            Serializable value = null;
            if (fs != null) {
                value = adapter.getFeatureValue(feature, fs);
            }

            FeatureState featureState = new FeatureState(vid, feature, value);
            featureStates.add(featureState);
            featureState.tagset = annotationService.listTags(featureState.feature.getTagset());
        }

        return featureStates;
    }
    
    private void addAnnotateActionBehavior(final FeatureEditor aFrag)
    {
        aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 5179816588460867471L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
            {
                super.updateAjaxAttributes(aAttributes);
                // When focus is on a feature editor and the user selects a new annotation,
                // there is a race condition between the saving the value of the feature
                // editor and the loading of the new annotation. Delay the feature editor
                // save to give preference to loading the new annotation.
                aAttributes.setThrottlingSettings(new ThrottlingSettings(getMarkupId(),
                    Duration.milliseconds(250), true));
                aAttributes.getAjaxCallListeners().add(new AjaxCallListener()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CharSequence getPrecondition(Component aComponent)
                    {
                        // If the panel refreshes because the user selects a new annotation,
                        // the annotation editor panel is updated for the new annotation
                        // first (before saving values) because of the delay set above. When
                        // the delay is over, we can no longer save the value because the
                        // old component is no longer there. We use the markup id of the
                        // editor fragments to check if the old component is still there
                        // (i.e. if the user has just tabbed to a new field) or if the old
                        // component is gone (i.e. the user selected/created another
                        // annotation). If the old component is no longer there, we abort
                        // the delayed save action.
                        return "return $('#" + aFrag.getMarkupId() + "').length > 0;";
                    }
                });
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                actionAnnotate(aTarget);
            }
        });
    }
    
    private void actionAnnotate(AjaxRequestTarget aTarget)
    {
        try {
            // When updating an annotation in the sidebar, we must not force a
            // re-focus after rendering
            getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);
            
            // Load the boiler-plate
            CAS cas = jcasProvider.get();
            FeatureStructure fs = selectFsByAddr(cas, getModelObject().getId());
            AnnotationLayer layer = annotationService.findLayer(project.getObject(), fs);
            TypeAdapter adapter = annotationService.getAdapter(layer);

            // Update the features of the selected annotation from the values presently in
            // the feature editors
            writeFeatureEditorModelsToCas(adapter, cas);
            
            // persist changes
            annotationPage.writeEditorCas(cas);
        }
        catch (Exception e) {
            handleException(DocumentMetadataAnnotationDetailPanel.this, aTarget, e);
        }
    }
    
    private void actionDelete(AjaxRequestTarget aTarget)
    {
        try {
            // Load the boiler-plate
            CAS cas = jcasProvider.get();
            FeatureStructure fs = selectFsByAddr(cas, getModelObject().getId());
            AnnotationLayer layer = annotationService.findLayer(project.getObject(), fs);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            
            // Perform actual actions
            adapter.delete(sourceDocument.getObject(), username.getObject(), cas, new VID(fs));
            
            // persist changes
            annotationPage.writeEditorCas(cas);
            
            aTarget.add(getParent());
            
            selectionPanel.actionDelete(aTarget, this);
        }
        catch (Exception e) {
            handleException(DocumentMetadataAnnotationDetailPanel.this, aTarget, e);
        }
    }
    
    private void writeFeatureEditorModelsToCas(TypeAdapter aAdapter, CAS aCas)
            throws IOException
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
            aAdapter.setFeatureValue(sourceDocument.getObject(), username.getObject(), aCas,
                    getModelObject().getId(), featureState.feature, featureState.value);
        }
    }
    
    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        if (aTarget != null) {
            aTarget.addChildren(aComponent.getPage(), IFeedback.class);
        }
        
        try {
            throw aException;
        }
        catch (AnnotationException e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
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
        setVisible(!isVisible());
    }
    
    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        try {
            CAS cas = jcasProvider.get();
            AnnotationFS fs =
                selectAnnotationByAddr(cas, aEvent.getLinkWithRoleModel().targetAddr);
            state.getSelection().selectSpan(fs);
            if (state.getSelection().getAnnotation().isSet()) {
                actionHandler.actionDelete(target);
            }
        }
        catch (IOException | AnnotationException e) {
            handleException(this, target, e);
        }
    }
    
    @OnEvent(stop = true)
    public void onFeatureUpdatedEvent(FeatureEditorValueChangedEvent aEvent)
    {
        actionAnnotate(aEvent.getTarget());
    }
}
