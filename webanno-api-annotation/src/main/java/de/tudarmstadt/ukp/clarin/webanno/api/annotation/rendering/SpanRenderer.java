/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanLayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

/**
 * Render spans.
 */
public class SpanRenderer
    extends Renderer_ImplBase<SpanAdapter>
{
    private final List<SpanLayerBehavior> behaviors;
    
    public SpanRenderer(SpanAdapter aTypeAdapter, FeatureSupportRegistry aFeatureSupportRegistry,
            List<SpanLayerBehavior> aBehaviors)
    {
        super(aTypeAdapter, aFeatureSupportRegistry);
        
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
    public void render(CAS aCas, List<AnnotationFeature> aFeatures,
            VDocument aResponse, int aWindowBegin, int aWindowEnd)
    {
        SpanAdapter typeAdapter = getTypeAdapter();
        
        List<AnnotationFeature> visibleFeatures = aFeatures.stream()
                .filter(f -> f.isVisible() && f.isEnabled())
                .collect(Collectors.toList());

        // Index mapping annotations to the corresponding rendered spans
        Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
        
        // Iterate over the span annotations of the current type and render each of them
        Type type = getType(aCas, typeAdapter.getAnnotationTypeName());
        List<AnnotationFS> annotations = selectCovered(aCas, type, aWindowBegin, aWindowEnd);
        for (AnnotationFS fs : annotations) {
            String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);
            Map<String, String> features = getFeatures(typeAdapter, fs, visibleFeatures);
            Map<String, String> hoverFeatures = getHoverFeatures(typeAdapter, fs, aFeatures);
            
            VRange range = new VRange(fs.getBegin() - aWindowBegin, fs.getEnd() - aWindowBegin);
            VSpan span = new VSpan(typeAdapter.getLayer(), fs, bratTypeName, range, features,
                    hoverFeatures);
            
            annoToSpanIdx.put(fs, span);
            
            aResponse.add(span);
            
            // Render errors if required features are missing
            renderRequiredFeatureErrors(visibleFeatures, fs, aResponse);

            // Render slots
            int fi = 0;
            for (AnnotationFeature feat : typeAdapter.listFeatures()) {
                if (MultiValueMode.ARRAY.equals(feat.getMultiValueMode())
                        && LinkMode.WITH_ROLE.equals(feat.getLinkMode())) {
                    List<LinkWithRoleModel> links = typeAdapter.getFeatureValue(feat, fs);
                    for (int li = 0; li < links.size(); li++) {
                        LinkWithRoleModel link = links.get(li);
                        FeatureStructure targetFS = selectFsByAddr(fs.getCAS(), link.targetAddr);
                        aResponse.add(new VArc(typeAdapter.getLayer(), new VID(fs, fi, li),
                                bratTypeName, fs, targetFS, link.role, features));
                    }
                }
                fi++;
            }
        }
        
        for (SpanLayerBehavior behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx, aWindowBegin, aWindowEnd);
        }
    }
}
