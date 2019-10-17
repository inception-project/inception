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

import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class DocumentMetadataAnnotationSelectionPanel extends Panel
{
    private static final long serialVersionUID = 8318858582025740458L;

    private static final Logger LOG = LoggerFactory
            .getLogger(DocumentMetadataAnnotationSelectionPanel.class);
    
    private static final String CID_LABEL = "label";
    private static final String CID_ANNOTATION_LINK = "annotationLink";
    private static final String CID_TYPE = "type";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_LAYER = "layer";
    private static final String CID_CREATE = "create";
    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";
    
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    
    private final AnnotationPage annotationPage;
    private final CasProvider jcasProvider;
    private final IModel<Project> project;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<String> username;
    private final IModel<AnnotationLayer> selectedLayer;
    private final WebMarkupContainer annotationsContainer;
    private final AnnotationActionHandler actionHandler;
    private List<DocumentMetadataAnnotationDetailPanel> detailPanels;
    private final AnnotatorState state;
    
    public DocumentMetadataAnnotationSelectionPanel(String aId, IModel<Project> aProject,
            IModel<SourceDocument> aDocument, IModel<String> aUsername,
            CasProvider aCasProvider, AnnotationPage aAnnotationPage,
            AnnotationActionHandler aActionHandler, AnnotatorState aState)
    {
        super(aId, aProject);

        setOutputMarkupPlaceholderTag(true);
        
        annotationPage = aAnnotationPage;
        sourceDocument = aDocument;
        username = aUsername;
        jcasProvider = aCasProvider;
        project = aProject;
        selectedLayer = Model.of();
        actionHandler = aActionHandler;
        detailPanels = new ArrayList<>();
        state = aState;

        annotationsContainer = new WebMarkupContainer(CID_ANNOTATIONS_CONTAINER);
        annotationsContainer.setOutputMarkupId(true);
        annotationsContainer.add(createAnnotationList());
        add(annotationsContainer);
        
        DropDownChoice<AnnotationLayer> layer = new BootstrapSelect<>(CID_LAYER);
        layer.setModel(selectedLayer);
        layer.setChoices(this::listMetadataLayers);
        layer.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layer.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(layer);
        
        add(new LambdaAjaxLink(CID_CREATE, this::actionCreate));
    }
    
    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }
    
    private void actionCreate(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        DocumentMetadataLayerAdapter adapter = (DocumentMetadataLayerAdapter) annotationService
                .getAdapter(selectedLayer.getObject());
        CAS cas = jcasProvider.get();
        AnnotationBaseFS fs = adapter.add(sourceDocument.getObject(), username.getObject(), cas);
        
        annotationPage.writeEditorCas(cas);
        
        // close all other panels so that this is the only one opened after creation
        detailPanels.forEach(d -> d.setVisible(false));
        
        aTarget.add(this);
    }
    
    private void actionSelect(
        AjaxRequestTarget aTarget, DocumentMetadataAnnotationDetailPanel aDetailPanel)
    {
        // close all other detail panels that are open
        for (DocumentMetadataAnnotationDetailPanel detailPanel : detailPanels) {
            if (!(detailPanel == aDetailPanel) && detailPanel.isVisible()) {
                detailPanel.toggleVisibility();
                aTarget.add(detailPanel);
            }
        }
        aDetailPanel.toggleVisibility();
        aTarget.add(aDetailPanel);
    }
    
    void actionDelete(AjaxRequestTarget aTarget, DocumentMetadataAnnotationDetailPanel detailPanel)
    {
        detailPanels.remove(detailPanel);
        remove(detailPanel);
        aTarget.add(this);
    }
    
    private ListView<AnnotationListItem> createAnnotationList()
    {
        DocumentMetadataAnnotationSelectionPanel selectionPanel = this;
        detailPanels = new ArrayList<>();
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS,
                LoadableDetachableModel.of(this::listAnnotations))
        {
            private static final long serialVersionUID = -6833373063896777785L;
    
            /**
             * Determines if new annotations should be rendered visible or not.
             * For the initialization of existing annotations this value should be false.
             * Afterwards when manually creating new annotations it should be true to immediately
             * open them afterwards.
             * If there are no annotations at initialization it is initialized with true else false.
             */
            boolean renderVisible = getModelObject().size() == 0;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                aItem.add(new Label(CID_TYPE, aItem.getModelObject().layer.getUiName()));

                VID vid = new VID(aItem.getModelObject().addr);
    
                DocumentMetadataAnnotationDetailPanel detailPanel;
                // see if this detail panel already is instantiated, if so use it
                Optional<DocumentMetadataAnnotationDetailPanel> oldPanel =
                    detailPanels.stream().filter(d -> d.getModelObject().equals(vid)).findFirst();
                
                if (oldPanel.isPresent()) {
                    detailPanel = oldPanel.get();
                } else {
                    detailPanel = new DocumentMetadataAnnotationDetailPanel(
                        CID_ANNOTATION_DETAILS, Model.of(vid), sourceDocument, username,
                        jcasProvider, project, annotationPage, selectionPanel, actionHandler,
                        state);
                    detailPanel.setVisible(renderVisible);
                    detailPanels.add(detailPanel);
                }
                aItem.add(detailPanel);
                
                LambdaAjaxLink link = new LambdaAjaxLink(CID_ANNOTATION_LINK,
                    _target -> actionSelect(_target, detailPanel));
                link.add(new Label(CID_LABEL));
                aItem.add(link);
    
                aItem.setOutputMarkupId(true);
                
                // after all panels are created for existing annotations set renderVisible to true
                if (detailPanels.size() == getModelObject().size()) {
                    renderVisible = true;
                }
            }
        };
    }
    
    private List<AnnotationLayer> listMetadataLayers()
    {
        return annotationService.listAnnotationLayer(getModelObject()).stream()
                .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType())
                        && layer.isEnabled()).
                collect(Collectors.toList());
    }
    
    private List<AnnotationListItem> listAnnotations()
    {
        CAS cas;
        try {
            cas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }
        
        List<AnnotationListItem> items = new ArrayList<>();
        for (AnnotationLayer layer : listMetadataLayers()) {            
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            Renderer renderer = layerSupportRegistry.getLayerSupport(layer).getRenderer(layer);
            
            for (FeatureStructure fs : selectFS(cas, adapter.getAnnotationType(cas))) {
                Map<String, String> renderedFeatures = renderer.getFeatures(adapter, fs, features);
                String labelText = TypeUtil.getUiLabelText(adapter, renderedFeatures);
                if (labelText.isEmpty()) {
                    labelText = "(" + layer.getUiName() + ")";
                }
                items.add(new AnnotationListItem(WebAnnoCasUtil.getAddr(fs), labelText, layer));
            }
        }
        
        return items;
    }
    
    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        // If a feature value is updated refresh the annotation list since it might mean that
        // a label has changed
        aEvent.getRequestTarget().add(annotationsContainer);
    }
    
    private class AnnotationListItem
    {
        final int addr;
        final String label;
        final AnnotationLayer layer;
        
        public AnnotationListItem(int aAddr, String aLabel, AnnotationLayer aLayer)
        {
            super();
            addr = aAddr;
            label = aLabel;
            layer = aLayer;
        }
    }
}
