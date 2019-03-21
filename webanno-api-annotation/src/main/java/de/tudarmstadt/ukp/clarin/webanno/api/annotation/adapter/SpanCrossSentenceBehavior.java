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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType.ERROR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isBeginEndInSameSentence;
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
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Ensure that annotations do not cross sentence boundaries. For chain layers, this check applies
 * only to the chain elements. Chain links can still cross sentence boundaries.
 */
@Component
public class SpanCrossSentenceBehavior
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
        if (aAdapter.getLayer().isCrossSentence()) {
            return;
        }
        
        // Since we split spans into multiple ranges at sentence boundaries, we can simply check
        // if there are multiple ranges for a given span. This is cheaper than checking for
        // every annotation whether the begin/end offset is in the same sentence.
        for (Entry<AnnotationFS, VSpan> e : annoToSpanIdx.entrySet()) {
            if (e.getValue().getRanges().size() > 1) {
                aResponse.add(new VComment(new VID(e.getKey()), ERROR,
                        "Crossing sentence bounardies is not permitted."));
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

        // Prepare feedback messsage list
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
                        LogMessage.error(this, "Crossing sentence bounardies is not permitted."),
                        fs));
            }
        }

        return messages;
    }
}
