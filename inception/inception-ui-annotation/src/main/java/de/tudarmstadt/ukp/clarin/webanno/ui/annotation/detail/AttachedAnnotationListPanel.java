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

import static de.tudarmstadt.ukp.inception.rendering.vmodel.VID.NONE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.LowLevelException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation;
import de.tudarmstadt.ukp.inception.schema.api.AttachedAnnotation.Direction;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class AttachedAnnotationListPanel
    extends Panel
{
    private static final long serialVersionUID = 2229431469432051779L;

    private static final Logger LOG = LoggerFactory.getLogger(AttachedAnnotationListPanel.class);

    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean LayerSupportRegistry layerRegistry;

    private final AnnotationPageBase page;
    private final WebMarkupContainer noAttachedAnnotationsInfo;
    private final WebMarkupContainer attachedAnnotationsContainer;
    private final AnnotationActionHandler actionHandler;

    private final IModel<List<AttachedAnnotationInfo>> annotations;

    public AttachedAnnotationListPanel(String aId, AnnotationPageBase aPage,
            AnnotationActionHandler aActionHandler, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        page = aPage;
        annotations = LoadableDetachableModel.of(this::getRelationInfo);
        actionHandler = aActionHandler;

        noAttachedAnnotationsInfo = new WebMarkupContainer("noAttachedAnnotationsInfo");
        noAttachedAnnotationsInfo.setOutputMarkupPlaceholderTag(true);
        noAttachedAnnotationsInfo.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getSelection().isSet()
                        && getModelObject().getSelection().getAnnotation().getId() != NONE
                        && SpanLayerSupport.TYPE
                                .equals(getModelObject().getSelectedAnnotationLayer().getType())
                        && annotations.getObject().isEmpty()));
        add(noAttachedAnnotationsInfo);

        attachedAnnotationsContainer = new WebMarkupContainer("attachedAnnotationsContainer");
        attachedAnnotationsContainer.setOutputMarkupPlaceholderTag(true);
        attachedAnnotationsContainer.add(new AttachedAnnotationList("annotations", annotations));
        attachedAnnotationsContainer.add(visibleWhen(() -> !annotations.getObject().isEmpty()));
        add(attachedAnnotationsContainer);
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

    private List<AttachedAnnotationInfo> getRelationInfo()
    {
        Selection selection = getModelObject().getSelection();

        if (!selection.isSet() || selection.getAnnotation().getId() == NONE) {
            return Collections.emptyList();
        }

        CAS cas;
        try {
            cas = page.getEditorCas();
        }
        catch (IOException e) {
            // If we have trouble accessing the CAS, we probably never get here anyway...
            // the AnnotationPageBase should already have found the issue and displayed some
            // error to the user.
            LOG.error("Unable to access editor CAS", e);
            return Collections.emptyList();
        }

        AnnotationFS annoFs;
        try {
            annoFs = ICasUtil.selectAnnotationByAddr(cas, selection.getAnnotation().getId());
        }
        catch (LowLevelException e) {
            LOG.error("Annotation with ID [{}] not found", selection.getAnnotation().getId());
            return Collections.emptyList();
        }

        var localVid = VID.of(annoFs);

        var attachedAnnotations = new ArrayList<AttachedAnnotation>();
        attachedAnnotations.addAll(schemaService
                .getAttachedRels(getModelObject().getSelectedAnnotationLayer(), annoFs));
        attachedAnnotations.addAll(schemaService
                .getAttachedLinks(getModelObject().getSelectedAnnotationLayer(), annoFs));

        var featureCache = new HashMap<AnnotationLayer, List<AnnotationFeature>>();
        var rendererCache = new HashMap<AnnotationLayer, Renderer>();
        var adapterCache = new HashMap<AnnotationLayer, TypeAdapter>();

        var result = new ArrayList<AttachedAnnotationInfo>();
        for (var rel : attachedAnnotations) {
            var layer = rel.getLayer();
            var features = featureCache.get(layer);
            TypeAdapter adapter;
            Renderer renderer;
            if (features == null) {
                features = schemaService.listSupportedFeatures(layer);
                featureCache.put(layer, features);

                adapter = schemaService.getAdapter(layer);
                adapterCache.put(layer, adapter);

                LayerSupport<?, ?> layerSupport = layerRegistry.getLayerSupport(layer);
                renderer = layerSupport.createRenderer(layer, () -> featureCache.get(layer));
                rendererCache.put(layer, renderer);
            }
            else {
                adapter = adapterCache.get(layer);
                renderer = rendererCache.get(layer);
            }

            Map<String, String> renderedFeatures;
            if (rel.getRelation() != null) {
                renderedFeatures = renderer.renderLabelFeatureValues(adapter, rel.getRelation(),
                        features);
            }
            else {
                renderedFeatures = renderer.renderLabelFeatureValues(adapter, rel.getEndpoint(),
                        features);
            }

            var labelText = TypeUtil.getUiLabelText(renderedFeatures);

            labelText = isEmpty(labelText) //
                    ? rel.getLayer().getUiName() //
                    : rel.getLayer().getUiName() + ": " + labelText;

            result.add(new AttachedAnnotationInfo(layer, localVid,
                    rel.getRelation() != null ? VID.of(rel.getRelation()) : null,
                    VID.of(rel.getEndpoint()), labelText, rel.getEndpoint().getCoveredText(),
                    rel.getDirection()));
        }

        return result;
    }

    @SuppressWarnings("unused")
    private class AttachedAnnotationInfo
        implements Serializable
    {
        private static final long serialVersionUID = -6096671317063130452L;

        private final AnnotationLayer layer;
        private final VID localVid;
        private final VID relationVid;
        private final VID endPointVid;
        private final String label;
        private final String endpointText;
        private final Direction direction;

        public AttachedAnnotationInfo(AnnotationLayer aLayer, VID aLocalVid, VID aRelationVid,
                VID aEndPointVid, String aLabel, String aEndpointText, Direction aDirection)
        {
            layer = aLayer;
            localVid = aLocalVid;
            relationVid = aRelationVid;
            endPointVid = aEndPointVid;
            label = aLabel;
            endpointText = aEndpointText;
            direction = aDirection;
        }
    }

    private class AttachedAnnotationList
        extends RefreshingView<AttachedAnnotationInfo>
    {
        private static final long serialVersionUID = -5522565328169426657L;

        public AttachedAnnotationList(String aId, IModel<List<AttachedAnnotationInfo>> aRelations)
        {
            super(aId, aRelations);
        }

        @SuppressWarnings("unchecked")
        public List<AttachedAnnotationInfo> getModelObject()
        {
            return (List<AttachedAnnotationInfo>) getDefaultModelObject();
        }

        @Override
        protected Iterator<IModel<AttachedAnnotationInfo>> getItemModels()
        {
            return new ModelIteratorAdapter<AttachedAnnotationInfo>(getModelObject())
            {
                @Override
                protected IModel<AttachedAnnotationInfo> model(AttachedAnnotationInfo aRelationInfo)
                {
                    return Model.of(aRelationInfo);
                }
            };
        }

        @Override
        protected void populateItem(Item<AttachedAnnotationInfo> aItem)
        {
            AttachedAnnotationInfo info = aItem.getModelObject();

            aItem.add(new Label("label", info.label));

            aItem.add(new Label("endpoint", info.endpointText));

            aItem.add(new LambdaAjaxLink("jumpToEndpoint",
                    _target -> actionHandler.actionSelectAndJump(_target, info.endPointVid))
                            .setAlwaysEnabled(true) // avoid disabling in read-only mode
            );

            LambdaAjaxLink selectRelation = new LambdaAjaxLink("selectRelation",
                    _target -> actionHandler.actionSelect(_target, info.relationVid));
            // avoid disabling in read-only mode
            selectRelation.setAlwaysEnabled(info.relationVid != null);
            selectRelation
                    .add(visibleWhen(() -> info.relationVid != null && info.relationVid.isSet()));
            aItem.add(selectRelation);

            selectRelation.add(new WebMarkupContainer("direction")
                    .add(new DirectionDecorator(info.direction)));
        }
    }

    private static class DirectionDecorator
        extends ClassAttributeModifier
    {
        private static final String ICON_MARKER_CLASS = "fas";
        private static final String ICON_LOOP = "fa-redo";
        private static final String ICON_OUTGOING = "fa-sign-out-alt";
        private static final String ICON_INCOMING = "fa-sign-in-alt";

        private static final long serialVersionUID = 7586775261302386820L;

        private final Direction direction;

        public DirectionDecorator(Direction aDirection)
        {
            direction = aDirection;
        }

        @Override
        protected Set<String> update(Set<String> aClasses)
        {
            aClasses.removeAll(asList(ICON_LOOP, ICON_INCOMING, ICON_OUTGOING));
            aClasses.add(ICON_MARKER_CLASS);
            switch (direction) {
            case INCOMING:
                aClasses.add(ICON_INCOMING);
                break;
            case OUTGOING:
                aClasses.add(ICON_OUTGOING);
                break;
            case LOOP:
                aClasses.add(ICON_LOOP);
                break;
            }

            return aClasses;
        }
    }
}
