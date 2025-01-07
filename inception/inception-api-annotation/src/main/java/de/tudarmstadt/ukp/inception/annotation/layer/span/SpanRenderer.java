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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationRenderer;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class SpanRenderer
    extends Renderer_ImplBase<SpanAdapter>
{
    private static final int MAX_HOVER_TEXT_LENGTH = 1000;

    private final List<SpanLayerBehavior> behaviors;

    private Type type;

    public SpanRenderer(SpanAdapter aTypeAdapter, LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, List<SpanLayerBehavior> aBehaviors)
    {
        super(aTypeAdapter, aLayerSupportRegistry, aFeatureSupportRegistry);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            var temp = new ArrayList<SpanLayerBehavior>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        var typeAdapter = getTypeAdapter();

        type = aTypeSystem.getType(typeAdapter.getAnnotationTypeName());
        if (type == null) {
            // If the types are not defined, then we do not need to try and render them because the
            // CAS does not contain any instances of them
            return false;
        }

        return true;
    }

    @Override
    public List<Annotation> selectAnnotationsInWindow(RenderRequest aRequest, int aWindowBegin,
            int aWindowEnd)
    {
        var cas = aRequest.getCas();

        if (!aRequest.isLongArcs()) {
            return cas.<Annotation> select(type) //
                    .coveredBy(0, aWindowEnd) //
                    .includeAnnotationsWithEndBeyondBounds() //
                    .filter(ann -> AnnotationPredicates.overlapping(ann, aWindowBegin, aWindowEnd))
                    .toList();
        }

        return aRequest.getCas().<Annotation> select(type).toList();
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(CAS aCas, VID aVid)
    {
        if (!checkTypeSystem(aCas)) {
            return Collections.emptyList();
        }

        var fs = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aVid.getId());

        var group = new VLazyDetailGroup();
        group.addDetail(
                new VLazyDetail("Text", abbreviate(fs.getCoveredText(), MAX_HOVER_TEXT_LENGTH)));

        if (aVid.getAttribute() != VID.NONE) {
            var adapter = getTypeAdapter();
            var feature = adapter.listFeatures().stream().sequential().skip(aVid.getAttribute())
                    .findFirst().get();
            List<LinkWithRoleModel> sourceLinks = adapter.getFeatureValue(feature, fs);
            var target = selectAnnotationByAddr(aCas, sourceLinks.get(aVid.getSlot()).targetAddr);
            group.addDetail(new VLazyDetail("Target",
                    abbreviate(target.getCoveredText(), MAX_HOVER_TEXT_LENGTH)));
        }

        var details = super.lookupLazyDetails(aCas, aVid);
        if (!group.getDetails().isEmpty()) {
            details.add(0, group);
        }
        return details;
    }

    @Override
    public void render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse)
    {
        if (!checkTypeSystem(aRequest.getCas())) {
            return;
        }

        var typeAdapter = getTypeAdapter();

        // Index mapping annotations to the corresponding rendered spans
        var annoToSpanIdx = new HashMap<AnnotationFS, VSpan>();

        var annotations = selectAnnotationsInWindow(aRequest, aResponse.getWindowBegin(),
                aResponse.getWindowEnd());

        for (var fs : annotations) {
            for (var vobj : render(aRequest, aFeatures, aResponse, fs)) {
                aResponse.add(vobj);

                if (vobj instanceof VSpan vspan) {
                    annoToSpanIdx.put(fs, vspan);

                    renderRequiredFeatureErrors(aRequest, aFeatures, fs, aResponse);
                }
            }
        }

        for (var behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx);
        }
    }

    @Override
    public List<VObject> render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse, AnnotationFS aFS)
    {
        if (!checkTypeSystem(aFS.getCAS())) {
            return emptyList();
        }

        var range = VRange.clippedRange(aResponse, aFS);

        var spansAndSlots = new ArrayList<VObject>();
        VID source;
        if (range.isPresent()) {
            var typeAdapter = getTypeAdapter();
            var labelFeatures = renderLabelFeatureValues(typeAdapter, aFS, aFeatures);
            var span = new VSpan(typeAdapter.getLayer(), aFS, range.get(), labelFeatures);
            source = span.getVid();
            spansAndSlots.add(span);
        }
        else {
            source = RelationRenderer.createEndpoint(aRequest, aResponse, aFS, getTypeAdapter());
        }

        renderSlots(aRequest, aResponse, aFS, source, spansAndSlots);

        return spansAndSlots;
    }

    private void renderSlots(RenderRequest aRequest, VDocument aVDocument, AnnotationFS aFS,
            VID aSource, List<VObject> aSpansAndSlots)
    {
        var typeAdapter = getTypeAdapter();
        var aWindowBegin = aVDocument.getWindowBegin();
        var aWindowEnd = aVDocument.getWindowEnd();
        var layer = typeAdapter.getLayer();

        int fi = 0;
        nextFeature: for (var feat : typeAdapter.listFeatures()) {
            if (!feat.isEnabled()) {
                fi++;
                continue nextFeature;
            }

            if (feat.getMultiValueMode() == ARRAY && feat.getLinkMode() == WITH_ROLE) {
                var traits = getTraits(feat, LinkFeatureTraits.class);

                List<LinkWithRoleModel> links = typeAdapter.getFeatureValue(feat, aFS);
                for (var li = 0; li < links.size(); li++) {
                    var link = links.get(li);
                    var targetFS = selectByAddr(aFS.getCAS(), Annotation.class, link.targetAddr);

                    var arcBegin = Math.min(aFS.getBegin(), targetFS.getBegin());
                    var arcEnd = Math.max(aFS.getEnd(), targetFS.getEnd());

                    if (overlapping(arcBegin, arcEnd, aWindowBegin, aWindowEnd)) {
                        var target = RelationRenderer.createEndpoint(aRequest, aVDocument, targetFS,
                                typeAdapter);

                        var vid = VID.builder().forAnnotation(aFS) //
                                .withAttribute(fi) //
                                .withSlot(li) //
                                .build();

                        var label = traits.map(LinkFeatureTraits::isEnableRoleLabels).orElse(false)
                                ? link.role
                                : feat.getUiName();

                        var arc = VArc.builder() //
                                .withLayer(layer) //
                                .withVid(vid) //
                                .withSource(aSource) //
                                .withTarget(target) //
                                .withLabel(label) //
                                .build();

                        aSpansAndSlots.add(arc);
                    }
                }
            }

            fi++;
        }
    }
}
