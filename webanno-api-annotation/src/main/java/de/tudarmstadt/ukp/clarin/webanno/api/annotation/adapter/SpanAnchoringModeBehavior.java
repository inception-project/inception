/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Order(10)
@Component
public class SpanAnchoringModeBehavior
    extends SpanLayerBehavior
{
    @Override
    public boolean accepts(LayerSupport<?> aLayerType)
    {
        return super.accepts(aLayerType) || aLayerType instanceof ChainLayerSupport;
    }
    
    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (aRequest.getBegin() == aRequest.getEnd()) {
            if (!aAdapter.getLayer().getAnchoringMode().isZeroSpanAllowed()) {
                throw new AnnotationException(
                        "Cannot create zero-width annotation on layers that lock to token boundaries.");
            }

            return aRequest;
        }
        
        int[] originalRange = new int[] { aRequest.getBegin(), aRequest.getEnd() };
        int[] adjustedRange = adjust(aRequest.getCas(), aAdapter.getLayer().getAnchoringMode(),
                originalRange);
        
        if (adjustedRange.equals(originalRange)) {
            return aRequest;
        }
        else {
            return aRequest.changeSpan(adjustedRange[0], adjustedRange[1]);
        }
    }
    
    public static int[] adjust(CAS aJCas, AnchoringMode aMode, int[] aRange)
        throws AnnotationException
    {
        switch (aMode) {
        case CHARACTERS: {
            return aRange;
        }
        case SINGLE_TOKEN: {
            Type tokenType = getType(aJCas, Token.class);
            List<AnnotationFS> tokens = selectOverlapping(aJCas, tokenType, aRange[0], aRange[1]);

            if (tokens.isEmpty()) {
                throw new AnnotationException(
                        "No tokens found int range [" + aRange[0] + "-" + aRange[1] + "]");
            }
                        
            return new int[] { tokens.get(0).getBegin(), tokens.get(0).getEnd() };
        }
        case TOKENS: {
            Type tokenType = getType(aJCas, Token.class);
            List<AnnotationFS> tokens = selectOverlapping(aJCas, tokenType, aRange[0], aRange[1]);

            if (tokens.isEmpty()) {
                throw new AnnotationException(
                        "No tokens found int range [" + aRange[0] + "-" + aRange[1] + "]");
            }

            // update the begin and ends (no sub token selection)
            int begin = tokens.get(0).getBegin();
            int end = tokens.get(tokens.size() - 1).getEnd();
            
            return new int[] { begin, end };
        }
        case SENTENCES: {
            Type sentenceType = getType(aJCas, Sentence.class);
            List<AnnotationFS> sentences = selectOverlapping(aJCas, sentenceType, aRange[0],
                    aRange[1]);
            
            if (sentences.isEmpty()) {
                throw new AnnotationException(
                        "No sentences found int range [" + aRange[0] + "-" + aRange[1] + "]");
            }
            
            // update the begin and ends (no sub token selection)
            int begin = sentences.get(0).getBegin();
            int end = sentences.get(sentences.size() - 1).getEnd();
            
            return new int[] { begin, end };
        }
        default:
            throw new IllegalArgumentException("Unsupported anchoring mode: [" + aMode + "]");
        }    
    }
}
