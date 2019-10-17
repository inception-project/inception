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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

public class DocumentAnnotationPanel 
    extends Panel
{

    private static final long serialVersionUID = -1661546571421782539L;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private static final String CID_ANNOTATION_TITLE = "annotationTitle";
    private static final String CID_FEATURES_CONTAINER = "featuresContainer";
    private static final String CID_FEATURES = "features";
    private static final String CID_LINK_FEATURES_CONTAINER = "linkFeaturesContainer";
    private static final String CID_LINK_FEATURES = "linkFeatures";
    private static final String CID_LINK_FEATURE_TITLE = "linkFeatureTitle";
    private static final String CID_SPANS_CONTAINER = "spansContainer";
    private static final String CID_SPANS = "spans";
    private static final String CID_SPAN = "span";
    private static final String CID_LABEL = "label";
    private static final String CID_VALUE = "value";

    private final CasProvider casProvider;
    private final WebMarkupContainer featuresContainer;
    private final WebMarkupContainer linkFeaturesContainer;
    private final IModel<VID> model;
    private final AnnotatorState state;

    public DocumentAnnotationPanel(String id, IModel<VID> aModel, CasProvider aCasProvider,
        AnnotatorState aState, String aTitle)
    {
        super(id, aModel);

        casProvider = aCasProvider;
        model = aModel;
        state = aState;

        // Allow AJAX updates.
        setOutputMarkupId(true);
    
        List<FeatureState> features = listFeatures();
        List<FeatureState> linkFeatures = new ArrayList<>();
        List<FeatureState> otherFeatures = new ArrayList<>();
    
        for (FeatureState featureState : features) {
            AnnotationFeature feature = featureState.feature;
            if (feature.getMultiValueMode().equals(MultiValueMode.ARRAY)
                && feature.getLinkMode().equals(LinkMode.WITH_ROLE)) {
                linkFeatures.add(featureState);
            } else {
                otherFeatures.add(featureState);
            }
        }
        
        featuresContainer = new WebMarkupContainer(CID_FEATURES_CONTAINER);
        featuresContainer.setOutputMarkupId(true);
        featuresContainer.add(createFeaturesList(otherFeatures));
        add(featuresContainer);
    
        linkFeaturesContainer = new WebMarkupContainer(CID_LINK_FEATURES_CONTAINER);
        linkFeaturesContainer.setOutputMarkupId(true);
        linkFeaturesContainer.add(createLinkFeaturesList(linkFeatures));
        add(linkFeaturesContainer);
        
        add(new Label(CID_ANNOTATION_TITLE, aTitle));
    }
    
    private List<FeatureState> listFeatures()
    {
        VID vid = model.getObject();
        
        if (state.getProject() == null || vid == null || vid.isNotSet()) {
            return emptyList();
        }
        
        CAS cas;
        try {
            cas = casProvider.get();
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
        AnnotationLayer layer = annotationService.findLayer(state.getProject(), fs);
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

    private ListView<FeatureState> createFeaturesList(List<FeatureState> features)
    {
        return new ListView<FeatureState>(CID_FEATURES, features)
        {
            private static final long serialVersionUID = -1139622234318691941L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                // Feature editors that allow multiple values may want to update themselves,
                // e.g. to add another slot.
                item.setOutputMarkupId(true);
    
    
                final FeatureState featureState = item.getModelObject();
    
                Label label = new Label(CID_LABEL, featureState.feature.getUiName() + ": ");
                Label value = new Label(CID_VALUE, featureState.value);
    
                item.add(label);
                item.add(value);
            }
        };
    }
    
    private ListView<FeatureState> createLinkFeaturesList(List<FeatureState> features)
    {
        return new ListView<FeatureState>(CID_LINK_FEATURES, features)
        {
            private static final long serialVersionUID = 2226005003861152513L;

            @Override protected void populateItem(ListItem<FeatureState> item)
            {
                // Feature editors that allow multiple values may want to update themselves,
                // e.g. to add another slot.
                item.setOutputMarkupId(true);
    
                final FeatureState featureState = item.getModelObject();
    
                List<LinkWithRoleModel> annotations =
                    (List<LinkWithRoleModel>) featureState.value;
                
                Label label =
                    new Label(CID_LINK_FEATURE_TITLE, featureState.feature.getUiName());
                item.add(label);
                
                WebMarkupContainer spansContainer =
                    new WebMarkupContainer(CID_SPANS_CONTAINER);
                spansContainer.setOutputMarkupId(true);
                spansContainer.add(createSpansList(annotations));
                item.add(spansContainer);
                item.add(visibleWhen(() -> annotations.size() > 0));
            }
        };
    }
    
    private ListView<LinkWithRoleModel> createSpansList(List<LinkWithRoleModel> annotations)
    {
        return new ListView<LinkWithRoleModel>(CID_SPANS, annotations)
        {
            private static final long serialVersionUID = -2402736599496484623L;
    
            @Override protected void populateItem(ListItem<LinkWithRoleModel> item)
            {
                SpanAnnotationPanel panel = new SpanAnnotationPanel(CID_SPAN,
                    item.getModel(), casProvider, state);
                item.add(panel);
            }
        };
    }
    
    @OnEvent
    public void onRefreshEvent(RefreshEvent event)
    {
        event.getTarget().add(this);
    }
}
