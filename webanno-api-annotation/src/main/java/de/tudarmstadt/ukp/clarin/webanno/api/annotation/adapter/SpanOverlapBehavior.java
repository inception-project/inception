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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
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
        final CAS aCas = aRequest.getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();
        Type type = getType(aCas, aAdapter.getAnnotationTypeName());
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
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
            Map<AnnotationFS, VSpan> aAnnoToSpanIdx, int aPageBegin, int aPageEnd)
    {
        if (aAnnoToSpanIdx.isEmpty()) {
            return;
        }
        
        // The following code requires annotations with the same offsets to be adjacent during 
        // iteration, so we sort the entries here
        AnnotationComparator cmp = new AnnotationComparator();
        final List<AnnotationFS> sortedSpans = aAnnoToSpanIdx.keySet().stream()
                .sorted(cmp)
                .collect(Collectors.toList());
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
            break;
        case NO_OVERLAP: {
            Set<AnnotationFS> overlapping = new HashSet<>();
            Set<AnnotationFS> stacking = new HashSet<>();
            
            overlappingOrStackingSpans(sortedSpans, stacking, overlapping);
            
            overlapping.forEach(fs -> aResponse
                    .add(new VComment(new VID(fs), ERROR, "Overlap is not permitted.")));
            
            stacking.forEach(fs -> aResponse
                    .add(new VComment(new VID(fs), ERROR, "Stacking is not permitted.")));
            break;
        }
        case STACKING_ONLY:
            // Here, we must find all overlapping relations because they are not permitted
            overlappingNonStackingSpans(sortedSpans)
                    .forEach(fs -> aResponse
                            .add(new VComment(new VID(fs), ERROR, "Only stacking is permitted.")));
            break;
        case OVERLAP_ONLY:
            stackingSpans(sortedSpans).forEach(fs -> aResponse
                    .add(new VComment(new VID(fs), ERROR, "Stacking is not permitted.")));
            break;
        }
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, CAS aCas)
    {
        Type type = getType(aCas, aAdapter.getAnnotationTypeName());
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        
        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
            break;
        case NO_OVERLAP: {
            Set<AnnotationFS> overlapping = new HashSet<>();
            Set<AnnotationFS> stacking = new HashSet<>();
            
            overlappingOrStackingSpans(select(aCas, type), stacking, overlapping);
            
            overlapping.forEach(fs -> Pair.of(LogMessage.error(this,
                    "Overlapping annotation at [%d-%d]", fs.getBegin(), fs.getEnd()), fs));

            stacking.forEach(fs -> messages.add(Pair.of(LogMessage.error(this,
                    "Stacked annotation at [%d-%d]", fs.getBegin(), fs.getEnd()), fs)));
            break;
        }
        case STACKING_ONLY: 
            // Here, we must find all overlapping relations because they are not permitted
            overlappingNonStackingSpans(select(aCas, type))
                    .forEach(fs -> Pair.of(LogMessage.error(this, "Overlapping annotation at [%d-%d]",
                            fs.getBegin(), fs.getEnd()), fs));
            break;
        case OVERLAP_ONLY:
            stackingSpans(select(aCas, type))
                    .forEach(fs -> messages.add(Pair.of(LogMessage.error(this,
                            "Stacked annotation at [%d-%d]", fs.getBegin(), fs.getEnd()), fs)));
            break;
        }

        return messages;
    }

    private void overlappingOrStackingSpans(Collection<AnnotationFS> aSpans,
            Collection<AnnotationFS> aStacking, Collection<AnnotationFS> aOverlapping)
    {
        for (AnnotationFS span1 : aSpans) {
            for (AnnotationFS span2 : aSpans) {
                if (span1.equals(span2)) {
                    continue;
                }
                
                if (stacking(span1, span2)) {
                    aStacking.add(span1);
                    aStacking.add(span2);
                }
                else if (overlapping(span1, span2)) {
                    aOverlapping.add(span1);
                    aOverlapping.add(span2);
                }
            }
        }
    }
    
    private Set<AnnotationFS> overlappingNonStackingSpans(Collection<AnnotationFS> aSpans)
    {
        Set<AnnotationFS> overlapping = new HashSet<>();
        for (AnnotationFS fs1 : aSpans) {
            for (AnnotationFS fs2 : aSpans) {
                if (fs1.equals(fs2)) {
                    continue;
                }
                
                if (overlapping(fs1, fs2) && !stacking(fs1, fs2)) {
                    overlapping.add(fs1);
                    overlapping.add(fs2);
                }
            }
        }
        return overlapping;
    }

    private Set<AnnotationFS> stackingSpans(Collection<AnnotationFS> aSpans)
    {
        // Since the annotations are sorted, we can easily find stacked annotation by scanning
        // through the entire list and checking if two adjacent annotations have the same
        // offsets
        Set<AnnotationFS> stacking = new HashSet<>();
        AnnotationFS prevFS = null;
        for (AnnotationFS fs : aSpans) {
            if (
                    prevFS != null && 
                    prevFS.getBegin() == fs.getBegin() && 
                    prevFS.getEnd() == fs.getEnd())
            {
                stacking.add(prevFS);
                stacking.add(fs);
            }
            
            prevFS = fs;
        }
        return stacking;
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
