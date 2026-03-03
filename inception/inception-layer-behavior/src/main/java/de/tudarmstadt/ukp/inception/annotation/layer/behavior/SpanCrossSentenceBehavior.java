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

import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.ERROR;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isBeginEndInSameSentence;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAnnotationRequest_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Ensure that annotations do not cross sentence boundaries. For chain layers, this check applies
 * only to the chain elements. Chain links can still cross sentence boundaries.
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#spanCrossSentenceBehavior}.
 * </p>
 */
public class SpanCrossSentenceBehavior
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

        if (aAdapter.getLayer().isCrossSentence()) {
            return aRequest;
        }

        if (!isBeginEndInSameSentence(aRequest.getCas(), aRequest.getBegin(), aRequest.getEnd())) {
            throw new MultipleSentenceCoveredException("Annotation covers multiple sentences, "
                    + "limit your annotation to single sentence!");
        }

        return aRequest;
    }

    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VSpan> annoToSpanIdx)
    {
        if (aAdapter.getLayer().isCrossSentence() || annoToSpanIdx.isEmpty()) {
            return;
        }

        CAS cas = annoToSpanIdx.entrySet().iterator().next().getKey().getCAS();

        // Build indexes to allow quickly looking up the sentence by its begin/end offsets. Since
        // The indexes are navigable, we can also find the sentences starting/ending closes to a
        // particular offset, even if it is not the start/end offset of a sentence.
        NavigableMap<Integer, AnnotationFS> sentBeginIdx = new TreeMap<>();
        NavigableMap<Integer, AnnotationFS> sentEndIdx = new TreeMap<>();
        for (AnnotationFS sent : selectOverlapping(cas, getType(cas, Sentence.class),
                aResponse.getWindowBegin(), aResponse.getWindowEnd())) {
            sentBeginIdx.put(sent.getBegin(), sent);
            sentEndIdx.put(sent.getEnd(), sent);
        }

        for (AnnotationFS fs : annoToSpanIdx.keySet()) {
            Entry<Integer, AnnotationFS> s1 = sentBeginIdx.floorEntry(fs.getBegin());
            Entry<Integer, AnnotationFS> s2 = sentEndIdx.ceilingEntry(fs.getEnd());

            if (s1 == null || s2 == null) {
                // Unable to determine any sentences overlapping with the annotation
                continue;
            }

            if (!WebAnnoCasUtil.isSame(s1.getValue(), s2.getValue())) {
                aResponse.add(new VComment(VID.of(fs), ERROR,
                        "Crossing sentence boundaries is not permitted."));
            }
        }
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, CAS aCas)
    {
        // If crossing sentence boundaries is permitted, then there is nothing to validate here
        if (aAdapter.getLayer().isCrossSentence()) {
            return emptyList();
        }

        Type type = getType(aCas, aAdapter.getAnnotationTypeName());

        // If there are no annotations on this layer, nothing to do
        Collection<AnnotationFS> annotations = select(aCas, type);
        if (annotations.isEmpty()) {
            return emptyList();
        }

        // Prepare feedback message list
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();

        // Build indexes to allow quickly looking up the sentence by its begin/end offsets. Since
        // The indexes are navigable, we can also find the sentences starting/ending closes to a
        // particular offset, even if it is not the start/end offset of a sentence.
        NavigableMap<Integer, AnnotationFS> sentBeginIdx = new TreeMap<>();
        NavigableMap<Integer, AnnotationFS> sentEndIdx = new TreeMap<>();
        for (AnnotationFS sent : select(aCas, getType(aCas, Sentence.class))) {
            sentBeginIdx.put(sent.getBegin(), sent);
            sentEndIdx.put(sent.getEnd(), sent);
        }

        for (AnnotationFS fs : annotations) {
            Entry<Integer, AnnotationFS> s1 = sentBeginIdx.floorEntry(fs.getBegin());
            Entry<Integer, AnnotationFS> s2 = sentEndIdx.ceilingEntry(fs.getEnd());

            if (s1 == null || s2 == null) {
                messages.add(Pair.of(LogMessage.error(this,
                        "Unable to determine any sentences overlapping with [%d-%d]", fs.getBegin(),
                        fs.getEnd()), fs));
                continue;
            }

            if (!WebAnnoCasUtil.isSame(s1.getValue(), s2.getValue())) {
                messages.add(Pair.of(
                        LogMessage.error(this, "Crossing sentence boundaries is not permitted."),
                        fs));
            }
        }

        return messages;
    }
}
