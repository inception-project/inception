/*
 * Copyright 2019
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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.AnnotationComparator;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Handles the {@link OverlapMode} setting for {@link WebAnnoConst#SPAN_TYPE span layers}.
 */
@Component
public class SpanOverlapBehavior
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
        final CAS aCas = aRequest.getJcas().getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();
        Type type = getType(aCas, aAdapter.getAnnotationTypeName());
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            return aRequest;
        case NO_OVERLAP:
            boolean hasAnyOverlapping = !selectOverlapping(aCas, type, aBegin, aEnd).isEmpty();
            if (hasAnyOverlapping) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - no overlap or stacking is allowed for this layer.");
            }
            break;
        case OVERLAP_ONLY:
            boolean hasStacking = !selectAt(aCas, type, aBegin, aEnd).isEmpty();
            if (hasStacking) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - stacking is not allowed for this layer.");
            }
            break;
        case STACKING_ONLY:
            boolean hasOverlapping = selectOverlapping(aCas, type, aBegin, aEnd).stream()
                .filter(fs -> !stacking(aRequest, fs))
                .findAny().isPresent();
            if (hasOverlapping) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - only stacking is allowed for this layer.");
            }
            break;
        }


        return aRequest;
    }

    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VSpan> aAnnoToSpanIdx)
    {
        if (aAnnoToSpanIdx.isEmpty()) {
            return;
        }
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
            break;
        case NO_OVERLAP: {
            Set<VID> overlapping = new HashSet<>();
            for (AnnotationFS fs1 : aAnnoToSpanIdx.keySet()) {
                for (AnnotationFS fs2 : aAnnoToSpanIdx.keySet()) {
                    if (overlapping(fs1, fs2)) {
                        overlapping.add(new VID(fs1));
                        overlapping.add(new VID(fs2));
                    }
                }
            }
            
            for (VID vid : overlapping) {
                aResponse.add(new VComment(vid, ERROR, "Overlaps are not permitted."));
            }
            break;
        }
        case STACKING_ONLY: {
            Set<VID> overlapping = new HashSet<>();
            for (AnnotationFS fs1 : aAnnoToSpanIdx.keySet()) {
                for (AnnotationFS fs2 : aAnnoToSpanIdx.keySet()) {
                    if (overlapping(fs1, fs2) && !stacking(fs1, fs2)) {
                        overlapping.add(new VID(fs1));
                        overlapping.add(new VID(fs2));
                    }
                }
            }
            
            for (VID vid : overlapping) {
                aResponse.add(new VComment(vid, ERROR, "Only stacking is permitted."));
            }
            break;
        }
        case OVERLAP_ONLY: {
            // The following code requires annotations with the same offsets to be adjacent during 
            // iteration, so we sort the entries here
            AnnotationComparator cmp = new AnnotationComparator();
            List<Entry<AnnotationFS, VSpan>> sortedEntries = aAnnoToSpanIdx.entrySet().stream()
                    .sorted((e1, e2) -> cmp.compare(e1.getKey(), e2.getKey()))
                    .collect(Collectors.toList());

            // Render error if annotations are stacked but stacking is not allowed
            AnnotationFS prevFS = null;
            boolean prevFSErrorGenerated = false;
            for (Entry<AnnotationFS, VSpan> e : sortedEntries) {
                AnnotationFS fs = e.getKey();
                if (
                        prevFS != null && 
                        prevFS.getBegin() == fs.getBegin() && 
                        prevFS.getEnd() == fs.getEnd()
                ) {
                    // If the current annotation is stacked with the previous one, generate an error
                    aResponse.add(new VComment(new VID(fs), ERROR, "Stacking is not permitted."));
                    
                    // If we did not already generate an error for the previous one, also generate
                    // an error for that one. This ensures that all stacked annotations get the 
                    // error marker, not only the 2nd, 3rd, and so on.
                    if (!prevFSErrorGenerated) {
                        aResponse.add(new VComment(new VID(prevFS), ERROR, "Stacking is not permitted."));
                    }
                }
                else {
                    prevFSErrorGenerated = false;
                }

                prevFS = fs;
            }
            break;
        }
        }
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, JCas aJCas)
    {
        CAS cas = aJCas.getCas();
        Type type = getType(cas, aAdapter.getAnnotationTypeName());
        AnnotationFS prevFS = null;
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
            break;
        case NO_OVERLAP: {
            throw new NotImplementedException("");
        }
        case STACKING_ONLY: {
            throw new NotImplementedException("");
        }
        case OVERLAP_ONLY: {
            // Since the annotations are sorted, we can easily find stacked annotation by scanning
            // through the entire list and checking if two adjacent annotations have the same
            // offsets
            for (AnnotationFS fs : select(cas, type)) {
                if (prevFS != null && prevFS.getBegin() == fs.getBegin()
                        && prevFS.getEnd() == fs.getEnd()) {
                    messages.add(Pair.of(LogMessage.error(this, "Stacked annotation at [%d-%d]",
                            fs.getBegin(), fs.getEnd()), fs));
                }
                
                prevFS = fs;
            }
            break;
        }
        }

        return messages;
    }
    
    public boolean overlapping(AnnotationFS aFS1, AnnotationFS aFS2)
    {
        return (aFS1.getBegin() <= aFS2.getBegin() && aFS2.getBegin() < aFS1.getEnd()) ||
                (aFS1.getBegin() <= aFS2.getEnd() && aFS2.getEnd() <= aFS1.getEnd());
    }
    
    public boolean stacking(CreateSpanAnnotationRequest aRequest, AnnotationFS aSpan)
    {
        return stacking(aRequest.getBegin(), aRequest.getEnd(), aSpan.getBegin(), aSpan.getEnd());
    }

    public boolean stacking(AnnotationFS aFS1, AnnotationFS aFS2)
    {
        return stacking(aFS1.getBegin(), aFS1.getEnd(), aFS2.getBegin(), aFS2.getEnd());
    }

    public boolean stacking(int aBegin1, int aEnd1, int aBegin2, int aEnd2)
    {
        return aBegin1 == aBegin2 && aEnd1 == aEnd2;
    }
}
