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
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
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
    public SpanRenderer(SpanAdapter aTypeAdapter, FeatureSupportRegistry aFeatureSupportRegistry)
    {
        super(aTypeAdapter, aFeatureSupportRegistry);
    }
    
    @Override
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            VDocument aResponse, int windowBeginOffset, int windowEndOffset)
    {
        List<AnnotationFeature> visibleFeatures = aFeatures.stream()
                .filter(f -> f.isVisible() && f.isEnabled()).collect(Collectors.toList());
        SpanAdapter typeAdapter = getTypeAdapter();
        Type type = getType(aJcas.getCas(), typeAdapter.getAnnotationTypeName());
        
        int windowBegin = windowBeginOffset;
        int windowEnd = windowEndOffset;

        List<Sentence> visibleSentences = selectCovered(aJcas, Sentence.class, windowBegin,
                windowEnd);
        
        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, windowBegin, windowEnd)) {
            String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);
            Map<String, String> features = getFeatures(typeAdapter, fs, visibleFeatures);
            Map<String, String> hoverFeatures = getHoverFeatures(typeAdapter, fs, aFeatures);
            
            Sentence beginSent = null;
            Sentence endSent = null;
            
            // check if annotation extends beyond viewable window - if yes, then constrain it to 
            // the visible window
            for (Sentence sent : visibleSentences) {
                if (beginSent == null) {
                    // Here we catch the first sentence in document order which covers the begin
                    // offset of the current annotation.
                    if (sent.getBegin() <= fs.getBegin() && fs.getBegin() <= sent.getEnd()) {
                        beginSent = sent;
                    }
                    // Make sure that zero-width annotations always start and end in the same
                    // sentence. Zero-width annotations that are on the boundary of two directly
                    // adjacent sentences (i.e. without whitespace between them) are considered
                    // to be at the end of the first sentence rather than at the beginning of the
                    // second sentence.
                    if (fs.getBegin() == fs.getEnd()) {
                        endSent = sent;
                    }
                }
                
                if (endSent == null) {
                    if (sent.getBegin() <= fs.getEnd() && fs.getEnd() <= sent.getEnd()) {
                        endSent = sent;
                    }
                }
                
                if (beginSent != null && endSent != null) {
                    break;
                }
            }
            
            if (beginSent == null || endSent == null) {
                throw new IllegalStateException(
                        "Unable to determine sentences in which the annotation starts/ends: " + fs);
            }

            List<Sentence> sentences = selectCovered(aJcas, Sentence.class, beginSent.getBegin(),
                    endSent.getEnd());
            List<VRange> ranges = new ArrayList<>();
            if (sentences.size() > 1) {
                for (Sentence sentence : sentences) {
                    if (sentence.getBegin() <= fs.getBegin() && fs.getBegin() < sentence.getEnd()) {
                        ranges.add(new VRange(fs.getBegin() - windowBegin,
                                sentence.getEnd() - windowBegin));
                    }
                    else if (sentence.getBegin() <= fs.getEnd()
                            && fs.getEnd() <= sentence.getEnd()) {
                        ranges.add(new VRange(sentence.getBegin() - windowBegin,
                                fs.getEnd() - windowBegin));
                    }
                    else {
                        ranges.add(new VRange(sentence.getBegin() - windowBegin,
                                sentence.getEnd() - windowBegin));
                    }
                }
                aResponse.add(
                        new VSpan(typeAdapter.getLayer(), fs, bratTypeName, ranges, features, 
                                hoverFeatures));
            }
            else {
                // FIXME It should be possible to remove this case and the if clause because
                // the case that a FS is inside a single sentence is just a special case
                aResponse.add(new VSpan(typeAdapter.getLayer(), fs, bratTypeName,
                        new VRange(fs.getBegin() - windowBegin, fs.getEnd() - windowBegin),
                        features, hoverFeatures));
            }
            
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
    }
}
