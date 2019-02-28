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
import java.util.stream.Collectors;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
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
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
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
    
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    
    private final AnnotationPage annotationPage;
    private final JCasProvider jcasProvider;
    private final DocumentMetadataAnnotationDetailPanel detailPanel;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<String> username;
    private final IModel<AnnotationLayer> selectedLayer;
    private final WebMarkupContainer annotationsContainer;
    
    public DocumentMetadataAnnotationSelectionPanel(String aId, IModel<Project> aProject,
            IModel<SourceDocument> aDocument, IModel<String> aUsername,
            JCasProvider aJCasProvider, DocumentMetadataAnnotationDetailPanel aDetails,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aProject);

        setOutputMarkupPlaceholderTag(true);
        
        annotationPage = aAnnotationPage;
        sourceDocument = aDocument;
        username = aUsername;
        jcasProvider = aJCasProvider;
        detailPanel = aDetails;
        selectedLayer = Model.of();

        annotationsContainer = new WebMarkupContainer("annotationsContainer");
        annotationsContainer.setOutputMarkupId(true);
        annotationsContainer.add(createAnnotationList());
        add(annotationsContainer);
        
        DropDownChoice<AnnotationLayer> layer = new BootstrapSelect<>("layer");
        layer.setModel(selectedLayer);
        layer.setChoices(this::listMetadataLayers);
        layer.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layer.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(layer);
        
        add(new LambdaAjaxLink("create", this::actionCreate));
    }
    
    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }
    
    private void actionCreate(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        DocumentMetadataLayerAdapter adapter = (DocumentMetadataLayerAdapter) annotationService
                .getAdapter(selectedLayer.getObject());
        JCas jcas = jcasProvider.get();
        AnnotationBaseFS fs = adapter.add(sourceDocument.getObject(), username.getObject(), jcas);
        detailPanel.setModelObject(new VID(fs));
        
        annotationPage.writeEditorCas(jcas);
        
        aTarget.add(this);
        aTarget.add(detailPanel);
    }
    
    private void actionSelect(AjaxRequestTarget aTarget, AnnotationListItem aItem)
    {
        detailPanel.setModelObject(new VID(aItem.addr));
        
        aTarget.add(detailPanel);
    }
    
    private ListView<AnnotationListItem> createAnnotationList()
    {
        return new ListView<AnnotationListItem>("annotations",
                LoadableDetachableModel.of(this::listAnnotations))
        {
            private static final long serialVersionUID = -6833373063896777785L;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                aItem.add(new Label("type", aItem.getModelObject().layer.getUiName()));

                LambdaAjaxLink link = new LambdaAjaxLink("annotationLink",
                    _target -> actionSelect(_target, aItem.getModelObject()));
                link.add(new Label("label"));
                aItem.add(link);
            }
        };
    }
    
    private List<AnnotationLayer> listMetadataLayers()
    {
        return annotationService.listAnnotationLayer(getModelObject()).stream()
                .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType())).
                collect(Collectors.toList());
    }
    
    private List<AnnotationListItem> listAnnotations()
    {
        JCas jcas;
        try {
            jcas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }
        
        List<AnnotationListItem> items = new ArrayList<>();
        for (AnnotationLayer layer : listMetadataLayers()) {
            if (!DocumentMetadataLayerSupport.TYPE.equals(layer.getType())) {
                continue;
            }
            
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            Renderer renderer = layerSupportRegistry.getLayerSupport(layer).getRenderer(layer);
            
            for (FeatureStructure fs : selectFS(jcas.getCas(),
                    adapter.getAnnotationType(jcas.getCas()))) {
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
