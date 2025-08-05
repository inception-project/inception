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
package de.tudarmstadt.ukp.inception.annotation.layer.behavior;

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.util.Arrays;

import org.apache.uima.cas.CAS;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAnnotationRequest_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#spanAnchoringModeBehavior}.
 * </p>
 */
@Order(100)
public class SpanAnchoringModeBehavior
    extends SpanLayerBehavior
{
    @Override
    public boolean accepts(LayerSupport<?, ?> aLayerType)
    {
        return super.accepts(aLayerType) || aLayerType instanceof ChainLayerSupport;
    }

    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        return onRequest(aAdapter, aRequest);
    }

    @Override
    public MoveSpanAnnotationRequest onMove(TypeAdapter aAdapter,
            MoveSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        return onRequest(aAdapter, aRequest);
    }

    private <T extends SpanAnnotationRequest_ImplBase<T>> T onRequest(TypeAdapter aAdapter,
            T aRequest)
        throws AnnotationException
    {
        if (Token.class.getName().equals(aAdapter.getAnnotationTypeName())
                || Sentence.class.getName().equals(aAdapter.getAnnotationTypeName())) {
            return aRequest;
        }

        if (aRequest.getBegin() == aRequest.getEnd()) {
            if (!aAdapter.getLayer().getAnchoringMode().isZeroSpanAllowed()) {
                throw new IllegalPlacementException(
                        "Cannot create zero-width annotation on layers that lock to token boundaries.");
            }

            return aRequest;
        }

        var anchoringMode = aAdapter.getLayer().getAnchoringMode();
        if (aRequest.getAnchoringMode() != null
                && anchoringMode.allows(aRequest.getAnchoringMode())) {
            anchoringMode = aRequest.getAnchoringMode();
        }

        int[] originalRange = new int[] { aRequest.getBegin(), aRequest.getEnd() };
        int[] adjustedRange = adjust(aRequest.getCas(), anchoringMode, originalRange);

        if (Arrays.equals(adjustedRange, originalRange)) {
            return aRequest;
        }

        return aRequest.changeSpan(adjustedRange[0], adjustedRange[1], anchoringMode);
    }

    static int[] adjust(CAS aCas, AnchoringMode aMode, int[] aRange) throws AnnotationException
    {
        return switch (aMode) {
        case CHARACTERS -> aRange;
        case SINGLE_TOKEN -> adjustToSingleToken(aCas, aRange);
        case TOKENS -> adjustToTokens(aCas, aRange);
        case SENTENCES -> adjustToSentences(aCas, aRange);
        default -> throw new IllegalArgumentException(
                "Unsupported anchoring mode: [" + aMode + "]");
        };
    }

    static int[] adjustToSentences(CAS aCas, int[] aRange) throws IllegalPlacementException
    {
        var sentenceType = getType(aCas, Sentence.class);
        var sentences = selectOverlapping(aCas, sentenceType, aRange[0], aRange[1]);

        if (sentences.isEmpty()) {
            throw new IllegalPlacementException(
                    "No sentences found int range [" + aRange[0] + "-" + aRange[1] + "]");
        }

        // update the begin and ends (no sub token selection)
        var begin = sentences.get(0).getBegin();
        var end = sentences.get(sentences.size() - 1).getEnd();

        return new int[] { begin, end };
    }

    static int[] adjustToTokens(CAS aCas, int[] aRange) throws IllegalPlacementException
    {
        var tokenType = getType(aCas, Token.class);
        var tokens = selectOverlapping(aCas, tokenType, aRange[0], aRange[1]);

        if (tokens.isEmpty()) {
            throw new IllegalPlacementException(
                    "No tokens found int range [" + aRange[0] + "-" + aRange[1] + "]");
        }

        // update the begin and ends (no sub token selection)
        int begin = tokens.get(0).getBegin();
        int end = tokens.get(tokens.size() - 1).getEnd();

        return new int[] { begin, end };
    }

    static int[] adjustToSingleToken(CAS aCas, int[] aRange) throws IllegalPlacementException
    {
        var tokenType = getType(aCas, Token.class);
        var tokens = selectOverlapping(aCas, tokenType, aRange[0], aRange[1]);

        if (tokens.isEmpty()) {
            throw new IllegalPlacementException(
                    "No tokens found int range [" + aRange[0] + "-" + aRange[1] + "]");
        }

        return new int[] { tokens.get(0).getBegin(), tokens.get(0).getEnd() };
    }
}
