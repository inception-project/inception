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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static java.util.Arrays.asList;
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
import org.apache.uima.fit.util.CasUtil;
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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

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
    public void render(CAS aJcas, List<AnnotationFeature> aFeatures,
            VDocument aResponse, int aWindowBegin, int aWindowEnd)
    {
        SpanAdapter typeAdapter = getTypeAdapter();
        
        List<AnnotationFeature> visibleFeatures = aFeatures.stream()
                .filter(f -> f.isVisible() && f.isEnabled())
                .collect(Collectors.toList());

        // Collect the visible sentences. The sentence boundary information is used to generate
        // multiple ranges for annotations crossing sentence boundaries
        List<AnnotationFS> visibleSentences = selectCovered(aJcas,
                CasUtil.getType(aJcas, Sentence.class), aWindowBegin, aWindowEnd);
        
        // Index mapping annotations to the corresponding rendered spans
        Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
        
        // Iterate over the span annotations of the current type and render each of them
        Type type = getType(aJcas, typeAdapter.getAnnotationTypeName());
        List<AnnotationFS> annotations = selectCovered(aJcas, type, aWindowBegin,
                aWindowEnd);
        for (AnnotationFS fs : annotations) {
            String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);
            Map<String, String> features = getFeatures(typeAdapter, fs, visibleFeatures);
            Map<String, String> hoverFeatures = getHoverFeatures(typeAdapter, fs, aFeatures);
            List<VRange> ranges = calculateRanges(aJcas, visibleSentences, aResponse,
                    aWindowBegin, aWindowEnd, fs);

            VSpan span = new VSpan(typeAdapter.getLayer(), fs, bratTypeName, ranges, features,
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
                        FeatureStructure targetFS = selectByAddr(fs.getCAS(), link.targetAddr);
                        aResponse.add(new VArc(typeAdapter.getLayer(), new VID(fs, fi, li),
                                bratTypeName, fs, targetFS, link.role, features));
                    }
                }
                fi++;
            }
        }
        
        for (SpanLayerBehavior behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToSpanIdx);
        }
    }
    
    private List<VRange> calculateRanges(CAS aJcas, List<AnnotationFS> aVisibleSentences,
            VDocument aResponse, int aWindowBegin, int aWindowEnd, AnnotationFS aFS)
    {
        AnnotationFS beginSent = null;
        AnnotationFS endSent = null;

        // check if annotation extends beyond viewable window - if yes, then constrain it to
        // the visible window
        for (AnnotationFS sent : aVisibleSentences) {
            if (beginSent == null) {
                // Here we catch the first sentence in document order which covers the begin
                // offset of the current annotation. Note that in UIMA annotations are
                // half-open intervals [begin,end) so that a begin offset must always be
                // smaller than the end of a covering annotation to be considered properly
                // covered.
                if (sent.getBegin() <= aFS.getBegin() && aFS.getBegin() < sent.getEnd()) {
                    beginSent = sent;
                }
                // Make sure that zero-width annotations always start and end in the same
                // sentence. Zero-width annotations that are on the boundary of two directly
                // adjacent sentences (i.e. without whitespace between them) are considered
                // to be at the end of the first sentence rather than at the beginning of the
                // second sentence.
                if (aFS.getBegin() == aFS.getEnd()) {
                    endSent = sent;
                }
            }

            if (endSent == null) {
                if (sent.getBegin() <= aFS.getEnd() && aFS.getEnd() <= sent.getEnd()) {
                    endSent = sent;
                }
            }

            if (beginSent != null && endSent != null) {
                break;
            }
        }

        if (beginSent == null || endSent == null) {
            throw new IllegalStateException(
                    "Unable to determine sentences in which the annotation starts/ends: " + aFS);
        }

        // If the annotation extends across sentence boundaries, create multiple ranges for the
        // annotation, one for every sentence.
        List<AnnotationFS> sentences = selectCovered(aJcas, getType(aJcas, Sentence.class),
                beginSent.getBegin(), endSent.getEnd());
        List<VRange> ranges = new ArrayList<>();
        if (sentences.size() > 1) {
            for (AnnotationFS sentence : sentences) {
                if (sentence.getBegin() <= aFS.getBegin() && aFS.getBegin() < sentence.getEnd()) {
                    ranges.add(new VRange(aFS.getBegin() - aWindowBegin,
                            sentence.getEnd() - aWindowBegin));
                }
                else if (sentence.getBegin() <= aFS.getEnd() && aFS.getEnd() <= sentence.getEnd()) {
                    ranges.add(new VRange(sentence.getBegin() - aWindowBegin,
                            aFS.getEnd() - aWindowBegin));
                }
                else {
                    ranges.add(new VRange(sentence.getBegin() - aWindowBegin,
                            sentence.getEnd() - aWindowBegin));
                }
            }

            return ranges;
        }
        else {
            return asList(
                    new VRange(aFS.getBegin() - aWindowBegin, aFS.getEnd() - aWindowBegin));
        }
    }
}
