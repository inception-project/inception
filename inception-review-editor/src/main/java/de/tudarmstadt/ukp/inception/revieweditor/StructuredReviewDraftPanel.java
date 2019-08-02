/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.revieweditor;

import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class StructuredReviewDraftPanel
    extends Panel
{
    private static final long serialVersionUID = 4202869513273132875L;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;

    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";

    private final CasProvider casProvider;
    private final WebMarkupContainer annotationsContainer;
    private final IModel<AnnotatorState> model;

    public StructuredReviewDraftPanel(String aId, IModel<AnnotatorState> aModel,
                                      CasProvider aCasProvider)
    {
        super(aId, aModel);
        
        casProvider = aCasProvider;
        model = aModel;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        annotationsContainer = new WebMarkupContainer(CID_ANNOTATIONS_CONTAINER);
        annotationsContainer.setOutputMarkupId(true);
        annotationsContainer.add(createAnnotationList());
        add(annotationsContainer);
    }

    private ListView<AnnotationListItem> createAnnotationList()
    {
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS,
            LoadableDetachableModel.of(this::listAnnotations))
        {
            private static final long serialVersionUID = -6833373063896777785L;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                String title = aItem.getModelObject().layer.getUiName();

                VID vid = new VID(aItem.getModelObject().addr);

                DocumentAnnotationPanel panel = 
                    new DocumentAnnotationPanel(CID_ANNOTATION_DETAILS, Model.of(vid), title);
                aItem.add(panel);

                aItem.setOutputMarkupId(true);
            }
        };
    }

    private List<AnnotationListItem> listAnnotations()
    {
        CAS cas;
        try {
            cas = casProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        List<AnnotationListItem> items = new ArrayList<>();
        List<AnnotationLayer> metadataLayers = 
            annotationService.listAnnotationLayer(model.getObject().getProject()).stream()
            .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType()))
            .collect(Collectors.toList());
        for (AnnotationLayer layer : metadataLayers) {
            if (!DocumentMetadataLayerSupport.TYPE.equals(layer.getType())) {
                continue;
            }

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
