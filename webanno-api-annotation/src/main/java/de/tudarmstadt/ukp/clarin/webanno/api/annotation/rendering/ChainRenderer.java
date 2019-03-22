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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanLayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class ChainRenderer
    extends Renderer_ImplBase<ChainAdapter>
{
    private final List<SpanLayerBehavior> behaviors;
    
    public ChainRenderer(ChainAdapter aTypeAdapter, FeatureSupportRegistry aFeatureSupportRegistry,
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
    public void render(CAS aCas, List<AnnotationFeature> aFeatures, VDocument aResponse,
            int windowBeginOffset, int windowEndOffset)
    {
        List<AnnotationFeature> visibleFeatures = aFeatures.stream()
                .filter(f -> f.isVisible() && f.isEnabled())
                .collect(Collectors.toList());
        
        // Find the features for the arc and span labels - it is possible that we do not find a
        // feature for arc/span labels because they may have been disabled.
        AnnotationFeature spanLabelFeature = null;
        AnnotationFeature arcLabelFeature = null;
        for (AnnotationFeature f : visibleFeatures) {
            if (WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(f.getName())) {
                spanLabelFeature = f;
            }
            if (WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(f.getName())) {
                arcLabelFeature = f;
            }
        }
        // At this point arc and span feature labels must have been found! If not, the later code
        // will crash.

        ChainAdapter typeAdapter = getTypeAdapter();
        Type chainType = CasUtil.getType(aCas, typeAdapter.getChainTypeName());
        Feature chainFirst = chainType.getFeatureByBaseName(typeAdapter.getChainFirstFeatureName());

        // Sorted index mapping annotations to the corresponding rendered spans
        Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
        
        int colorIndex = 0;
        // Iterate over the chains
        for (FeatureStructure chainFs : selectFS(aCas, chainType)) {
            AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
            AnnotationFS prevLinkFs = null;

            // Iterate over the links of the chain
            while (linkFs != null) {
                Feature linkNext = linkFs.getType()
                        .getFeatureByBaseName(typeAdapter.getLinkNextFeatureName());
                AnnotationFS nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);

                // Is link after window? If yes, we can skip the rest of the chain
                if (linkFs.getBegin() >= windowEndOffset) {
                    break; // Go to next chain
                }

                // Is link before window? We only need links that being within the window and that
                // end within the window
                if (!(linkFs.getBegin() >= windowBeginOffset)
                        && (linkFs.getEnd() <= windowEndOffset)) {
                    // prevLinkFs remains null until we enter the window
                    linkFs = nextLinkFs;
                    continue; // Go to next link
                }

                String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);

                // Render span
                {
                    String bratLabelText = TypeUtil.getUiLabelText(typeAdapter, linkFs,
                            (spanLabelFeature != null) ? asList(spanLabelFeature) : emptyList());
                    String bratHoverText = TypeUtil.getUiHoverText(typeAdapter, linkFs,
                            (spanLabelFeature != null) ? asList(spanLabelFeature) : emptyList());
                    VRange offsets = new VRange(linkFs.getBegin() - windowBeginOffset,
                            linkFs.getEnd() - windowBeginOffset);

                    VSpan span = new VSpan(typeAdapter.getLayer(), linkFs, bratTypeName, offsets,
                            colorIndex, singletonMap("label", bratLabelText), 
                            singletonMap("label", bratHoverText));
                    
                    annoToSpanIdx.put(linkFs, span);
                    
                    aResponse.add(span);
                }

                // Render arc (we do this on prevLinkFs because then we easily know that the current
                // and last link are within the window ;)
                if (prevLinkFs != null) {
                    String bratLabelText = null;

                    if (typeAdapter.isLinkedListBehavior() && arcLabelFeature != null) {
                        // Render arc label
                        bratLabelText = TypeUtil.getUiLabelText(typeAdapter, prevLinkFs,
                                asList(arcLabelFeature));
                    }
                    else {
                        // Render only chain type
                        bratLabelText = TypeUtil.getUiLabelText(typeAdapter, prevLinkFs,
                                emptyList());
                    }

                    aResponse.add(new VArc(typeAdapter.getLayer(),
                            new VID(prevLinkFs, 1, VID.NONE, VID.NONE), bratTypeName, prevLinkFs,
                            linkFs, colorIndex, singletonMap("label", bratLabelText)));
                }

                // Render errors if required features are missing
                renderRequiredFeatureErrors(visibleFeatures, linkFs, aResponse);

                prevLinkFs = linkFs;
                linkFs = nextLinkFs;
            }
            
            // The color index is updated even for chains that have no visible links in the current
            // window because we would like the chain color to be independent of visibility. In
            // particular the color of a chain should not change when switching pages/scrolling.
            colorIndex++;
        }
        
        for (SpanLayerBehavior behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx);
        }
    }
}
