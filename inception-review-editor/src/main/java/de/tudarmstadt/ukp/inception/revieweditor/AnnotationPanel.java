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
package de.tudarmstadt.ukp.inception.revieweditor;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static java.util.Collections.emptySet;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public abstract class AnnotationPanel
    extends Panel
{

    private static final long serialVersionUID = -5658080533124524441L;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private CasProvider casProvider;
    private IModel<AnnotatorState> model;

    public AnnotationPanel(String aId, IModel<AnnotatorState> aModel, CasProvider aCasProvider)
    {
        super(aId, aModel);
        model = aModel;
        casProvider = aCasProvider;
    }

    public List<AnnotationListItem> listDocumentAnnotations()
    {
        return listAnnotations().stream()
                .filter(a -> DocumentMetadataLayerSupport.TYPE.equals(a.getLayer().getType()))
                .collect(Collectors.toList());
    }

    public List<AnnotationListItem> listUnlinkedAnnotations()
    {
        List<AnnotationListItem> items = listAnnotations();
        List<AnnotationListItem> unlinkedItems = new ArrayList<>();
        Set<Integer> linkedAddr = new HashSet<>();

        for (AnnotationListItem item : items) {
            if (DocumentMetadataLayerSupport.TYPE.equals(item.getLayer().getType())) {
                VID vid = new VID(item.getAddr());
                linkedAddr.addAll(listLinkedFeatureAddresses(vid));
            }
        }

        for (AnnotationListItem item : items) {
            if (!DocumentMetadataLayerSupport.TYPE.equals(item.getLayer().getType())) {
                if (!linkedAddr.contains(item.getAddr())) {
                    unlinkedItems.add(item);
                }
            }
        }

        return unlinkedItems;
    }

    public List<AnnotationListItem> listAnnotations()
    {
        CAS cas = getCas();
        List<AnnotationListItem> items = new ArrayList<>();
        List<AnnotationLayer> metadataLayers = annotationService
                .listAnnotationLayer(model.getObject().getProject());

        for (AnnotationLayer layer : metadataLayers) {
            if (layer.getUiName().equals("Token")) {
                // TODO: exception later when calling renderer.getFeatures "lemma"
                continue;
            }

            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            Renderer renderer = layerSupportRegistry.getLayerSupport(layer).createRenderer(layer,
                    () -> annotationService.listAnnotationFeature(layer));

            for (FeatureStructure fs : selectFS(cas, adapter.getAnnotationType(cas))) {
                Map<String, String> renderedFeatures = renderer.renderLabelFeatureValues(adapter,
                        fs, features);
                String labelText = TypeUtil.getUiLabelText(adapter, renderedFeatures);
                if (labelText.isEmpty()) {
                    labelText = "(" + layer.getUiName() + ")";
                }
                items.add(new AnnotationListItem(WebAnnoCasUtil.getAddr(fs), labelText, layer));
            }
        }

        return items;
    }

    public Set<Integer> listLinkedFeatureAddresses(VID aVid)
    {
        Project project = getModel().getObject().getProject();
        if (project == null || aVid == null || aVid.isNotSet()) {
            return emptySet();
        }

        FeatureStructure fs;
        try {
            fs = selectFsByAddr(getCas(), aVid.getId());
        }
        catch (Exception e) {
            LOG.error("Unable to locate annotation with ID {}", aVid);
            return emptySet();
        }
        AnnotationLayer layer = annotationService.findLayer(project, fs);
        TypeAdapter adapter = annotationService.getAdapter(layer);

        // Populate from feature structure
        Set<Integer> linkedAddr = new HashSet<>();
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
            if (!feature.isEnabled() || !(feature.getMultiValueMode().equals(MultiValueMode.ARRAY)
                    && feature.getLinkMode().equals(LinkMode.WITH_ROLE))) {
                continue;
            }

            Serializable value = null;
            if (fs != null) {
                value = adapter.getFeatureValue(feature, fs);
            }

            linkedAddr.addAll(((List<LinkWithRoleModel>) value).stream()
                    .map(model -> model.targetAddr).collect(Collectors.toList()));
        }

        return linkedAddr;
    }

    public CasProvider getCasProvider()
    {
        return casProvider;
    }

    protected CAS getCas()
    {
        try {
            return casProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS.", e);
        }
        return null;
    }

    public IModel<AnnotatorState> getModel()
    {
        return model;
    }
}
