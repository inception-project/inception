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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.revieweditor.event.SelectAnnotationEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class SpanAnnotationPanel
    extends Panel
{
    private static final long serialVersionUID = 7375798934091777439L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private static final String CID_TEXT = "text";
    private static final String CID_FEATURES_CONTAINER = "featuresContainer";
    private static final String CID_TEXT_FEATURES = "textFeatures";
    private static final String CID_FEATURES = "features";
    private static final String CID_SELECT = "select";
    private static final String CID_LABEL = "label";
    private static final String CID_VALUE = "value";
    private static final String CID_PRE_CONTEXT = "preContext";
    private static final String CID_POST_CONTEXT = "postContext";

    private final AnnotatorState state;
    private WebMarkupContainer featuresContainer;

    public SpanAnnotationPanel(String aId, IModel<VID> aModel, CAS aCas, AnnotatorState aState)
    {
        super(aId, aModel);
        state = aState;

        var vid = aModel.getObject();

        var fs = ICasUtil.selectAnnotationByAddr(aCas, vid.getId());
        var layer = annotationService.findLayer(state.getProject(), fs);
        var begin = fs.getBegin();
        var end = fs.getEnd();

        var features = listFeatures(fs, layer, vid);
        var textFeatures = features.stream()
                .filter(featureState -> featureState.feature.getType().equals(CAS.TYPE_NAME_STRING)
                        && featureState.feature.getTagset() == null)
                .collect(Collectors.toList());
        features.removeAll(textFeatures);

        var selectButton = new LambdaAjaxLink(CID_SELECT, _target -> {
            send(this, BUBBLE, new SelectAnnotationEvent(vid, begin, end, _target));
            _target.add(this);
        });

        var text = aCas.getDocumentText();
        var windowSize = 50;
        var contextBegin = fs.getBegin() < windowSize ? 0 : fs.getBegin() - windowSize;
        var contextEnd = fs.getEnd() + windowSize > text.length() ? text.length()
                : fs.getEnd() + windowSize;
        var preContext = text.substring(contextBegin, fs.getBegin());
        var postContext = text.substring(fs.getEnd(), contextEnd);

        featuresContainer = new WebMarkupContainer(CID_FEATURES_CONTAINER);
        featuresContainer.setOutputMarkupId(true);
        featuresContainer.add(createTextFeaturesList(textFeatures));
        featuresContainer.add(createFeaturesList(features));
        featuresContainer.add(new Label(CID_PRE_CONTEXT, preContext));
        featuresContainer.add(new Label(CID_TEXT, fs.getCoveredText()));
        featuresContainer.add(new Label(CID_POST_CONTEXT, postContext));
        featuresContainer.add(selectButton);

        add(new ClassAttributeModifier()
        {
            private static final long serialVersionUID = -5391276660500827257L;

            @Override
            protected Set<String> update(Set<String> aClasses)
            {
                if (state.getSelection().getAnnotation().equals(vid)) {
                    aClasses.add("border");
                    aClasses.add("border-primary");
                }
                else {
                    aClasses.remove("border");
                    aClasses.remove("border-primary");
                }
                return aClasses;
            }
        });

        add(featuresContainer);
    }

    private List<FeatureState> listFeatures(FeatureStructure aFs, AnnotationLayer aLayer, VID aVid)
    {
        var adapter = annotationService.getAdapter(aLayer);

        // Populate from feature structure
        var featureStates = new ArrayList<FeatureState>();
        for (var feature : annotationService.listEnabledFeatures(aLayer)) {
            Serializable value = null;
            if (aFs != null) {
                value = adapter.getFeatureValue(feature, aFs);
            }

            var featureState = new FeatureState(aVid, feature, value);
            featureStates.add(featureState);
            featureState.tagset = annotationService
                    .listTagsReorderable(featureState.feature.getTagset());
        }

        return featureStates;
    }

    private ListView<FeatureState> createTextFeaturesList(List<FeatureState> features)
    {
        return new ListView<FeatureState>(CID_TEXT_FEATURES, features)
        {
            private static final long serialVersionUID = 2518085396361327922L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                populateFeatureItem(item);
            }
        };
    }

    private ListView<FeatureState> createFeaturesList(List<FeatureState> features)
    {
        return new ListView<FeatureState>(CID_FEATURES, features)
        {
            private static final long serialVersionUID = 16641722427333232L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                populateFeatureItem(item);
            }
        };
    }

    private void populateFeatureItem(ListItem<FeatureState> item)
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
}
