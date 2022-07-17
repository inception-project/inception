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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanLayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Render spans.
 */
public class SpanRenderer
    extends Renderer_ImplBase<SpanAdapter>
{
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
            List<SpanLayerBehavior> temp = new ArrayList<>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        SpanAdapter typeAdapter = getTypeAdapter();

        type = aTypeSystem.getType(typeAdapter.getAnnotationTypeName());
        if (type == null) {
            // If the types are not defined, then we do not need to try and render them because the
            // CAS does not contain any instances of them
            return false;
        }

        return true;
    }

    @Override
    public List<AnnotationFS> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        return aCas.select(type).coveredBy(0, aWindowEnd).includeAnnotationsWithEndBeyondBounds()
                .map(fs -> (AnnotationFS) fs)
                .filter(ann -> AnnotationPredicates.overlapping(ann, aWindowBegin, aWindowEnd))
                .collect(toList());
    }

    @Override
    public void render(CAS aCas, List<AnnotationFeature> aFeatures, VDocument aResponse,
            int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aCas)) {
            return;
        }

        SpanAdapter typeAdapter = getTypeAdapter();

        // Index mapping annotations to the corresponding rendered spans
        Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();

        List<AnnotationFS> annotations = selectAnnotationsInWindow(aCas, aWindowBegin, aWindowEnd);

        // List<AnnotationFS> annotations = selectCovered(aCas, type, aWindowBegin, aWindowEnd);
        for (AnnotationFS fs : annotations) {
            for (VObject vobj : render(aResponse, fs, aFeatures, aWindowBegin, aWindowEnd)) {
                aResponse.add(vobj);

                if (vobj instanceof VSpan) {
                    annoToSpanIdx.put(fs, (VSpan) vobj);

                    renderLazyDetails(fs, vobj, aFeatures);
                    renderRequiredFeatureErrors(aFeatures, fs, aResponse);
                }
            }
        }

        for (SpanLayerBehavior behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx, aWindowBegin, aWindowEnd);
        }
    }

    @Override
    public List<VObject> render(VDocument aVDocument, AnnotationFS aFS,
            List<AnnotationFeature> aFeatures, int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aFS.getCAS())) {
            return emptyList();
        }

        Optional<VRange> range = VRange.clippedRange(aVDocument, aFS);

        if (!range.isPresent()) {
            return emptyList();
        }

        List<VObject> spansAndSlots = new ArrayList<>();

        SpanAdapter typeAdapter = getTypeAdapter();
        String uiTypeName = typeAdapter.getEncodedTypeName();
        Map<String, String> labelFeatures = renderLabelFeatureValues(typeAdapter, aFS, aFeatures);

        VSpan span = new VSpan(typeAdapter.getLayer(), aFS, uiTypeName, range.get(), labelFeatures);
        spansAndSlots.add(span);

        renderSlots(aFS, spansAndSlots);

        return spansAndSlots;
    }

    private void renderSlots(AnnotationFS aFS, List<VObject> aSpansAndSlots)
    {
        SpanAdapter typeAdapter = getTypeAdapter();
        String uiTypeName = typeAdapter.getEncodedTypeName();

        int fi = 0;
        nextFeature: for (AnnotationFeature feat : typeAdapter.listFeatures()) {
            if (!feat.isEnabled()) {
                continue nextFeature;
            }

            if (ARRAY.equals(feat.getMultiValueMode()) && WITH_ROLE.equals(feat.getLinkMode())) {
                List<LinkWithRoleModel> links = typeAdapter.getFeatureValue(feat, aFS);
                for (int li = 0; li < links.size(); li++) {
                    LinkWithRoleModel link = links.get(li);
                    FeatureStructure targetFS = selectFsByAddr(aFS.getCAS(), link.targetAddr);
                    aSpansAndSlots.add(new VArc(typeAdapter.getLayer(), new VID(aFS, fi, li),
                            uiTypeName, aFS, targetFS, link.role));
                }
            }
            fi++;
        }
    }
}
