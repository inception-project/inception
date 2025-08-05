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
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAnnotationRequest_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparator;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Handles the {@link OverlapMode} setting for {@link SpanLayerSupport#TYPE span layers}.
 *
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#spanOverlapBehavior}.
 * </p>
 */
public class SpanOverlapBehavior
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
        try {
            aRequest.getCas().removeFsFromIndexes(aRequest.getAnnotation());
            return onRequest(aAdapter, aRequest);
        }
        finally {
            aRequest.getCas().addFsToIndexes(aRequest.getAnnotation());
        }
    }

    private <T extends SpanAnnotationRequest_ImplBase<T>> T onRequest(TypeAdapter aAdapter,
            T aRequest)
        throws AnnotationException
    {
        if (Token.class.getName().equals(aAdapter.getAnnotationTypeName())
                || Sentence.class.getName().equals(aAdapter.getAnnotationTypeName())) {
            return aRequest;
        }

        final CAS aCas = aRequest.getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();
        var type = getType(aCas, aAdapter.getAnnotationTypeName());

        switch (aAdapter.getLayer().getOverlapMode()) {
        case ANY_OVERLAP:
            // Nothing to check
            return aRequest;
        case NO_OVERLAP:
            boolean hasAnyOverlapping = !selectOverlapping(aCas, type, aBegin, aEnd).isEmpty();
            if (hasAnyOverlapping) {
                throw new IllegalPlacementException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - no overlap or stacking is allowed for this layer.");
            }
            break;
        case OVERLAP_ONLY:
            boolean hasStacking = !selectAt(aCas, type, aBegin, aEnd).isEmpty();
            if (hasStacking) {
                throw new IllegalPlacementException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - stacking is not allowed for this layer.");
            }
            break;
        case STACKING_ONLY:
            boolean hasOverlapping = selectOverlapping(aCas, type, aBegin, aEnd).stream()
                    .filter(fs -> !stacking(aRequest, fs)).findAny().isPresent();
            if (hasOverlapping) {
                throw new IllegalPlacementException("Cannot create another annotation of layer ["
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

        // The following code requires annotations with the same offsets to be adjacent during
        // iteration, so we sort the entries here
        var cmp = new AnnotationComparator();
        final List<AnnotationFS> sortedSpans = aAnnoToSpanIdx.keySet().stream().sorted(cmp)
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
                    .add(new VComment(VID.of(fs), ERROR, "Overlap is not permitted.")));

            stacking.forEach(fs -> aResponse
                    .add(new VComment(VID.of(fs), ERROR, "Stacking is not permitted.")));
            break;
        }
        case STACKING_ONLY:
            // Here, we must find all overlapping relations because they are not permitted
            overlappingNonStackingSpans(sortedSpans).forEach(fs -> aResponse
                    .add(new VComment(VID.of(fs), ERROR, "Only stacking is permitted.")));
            break;
        case OVERLAP_ONLY:
            stackingSpans(sortedSpans).forEach(fs -> aResponse
                    .add(new VComment(VID.of(fs), ERROR, "Stacking is not permitted.")));
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
                    .forEach(fs -> Pair.of(LogMessage.error(this,
                            "Overlapping annotation at [%d-%d]", fs.getBegin(), fs.getEnd()), fs));
            break;
        case OVERLAP_ONLY:
            stackingSpans(select(aCas, type))
                    .forEach(fs -> messages.add(Pair.of(LogMessage.error(this,
                            "Stacked annotation at [%d-%d]", fs.getBegin(), fs.getEnd()), fs)));
            break;
        }

        return messages;
    }

    static int overlappingOrStackingSpans(Collection<? extends AnnotationFS> aSpans,
            Collection<AnnotationFS> aStacking, Collection<AnnotationFS> aOverlapping)
    {
        var spans = aSpans.toArray(AnnotationFS[]::new);

        int n = 0;

        outer: for (int o = 0; o < spans.length - 1; o++) {
            var span1 = spans[o];

            inner: for (int i = o + 1; i < spans.length; i++) {
                var span2 = spans[i];

                n++;

                if (span2.getBegin() > span1.getEnd()) {
                    continue outer;
                }

                if (span1.equals(span2)) {
                    continue inner;
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

        return n;
    }

    static Set<AnnotationFS> overlappingNonStackingSpans(Collection<? extends AnnotationFS> aSpans)
    {
        var overlapping = new HashSet<AnnotationFS>();

        var spans = aSpans.toArray(AnnotationFS[]::new);

        outer: for (int o = 0; o < spans.length - 1; o++) {
            var span1 = spans[o];

            inner: for (int i = o + 1; i < spans.length; i++) {
                var span2 = spans[i];

                if (span2.getBegin() > span1.getEnd()) {
                    continue outer;
                }

                if (span1.equals(span2)) {
                    continue inner;
                }

                if (overlapping(span1, span2) && !stacking(span1, span2)) {
                    overlapping.add(span1);
                    overlapping.add(span2);
                }
            }
        }
        return overlapping;
    }

    static Set<AnnotationFS> stackingSpans(Collection<? extends AnnotationFS> aSpans)
    {
        // Since the annotations are sorted, we can easily find stacked annotation by scanning
        // through the entire list and checking if two adjacent annotations have the same
        // offsets
        var stacking = new HashSet<AnnotationFS>();
        AnnotationFS prevFS = null;
        for (var fs : aSpans) {
            if (prevFS != null && prevFS.getBegin() == fs.getBegin()
                    && prevFS.getEnd() == fs.getEnd()) {
                stacking.add(prevFS);
                stacking.add(fs);
            }

            prevFS = fs;
        }
        return stacking;
    }

    private static boolean overlapping(AnnotationFS aFS1, AnnotationFS aFS2)
    {
        return (aFS1.getBegin() <= aFS2.getBegin() && aFS2.getBegin() < aFS1.getEnd())
                || (aFS1.getBegin() < aFS2.getEnd() && aFS2.getEnd() <= aFS1.getEnd());
    }

    private static boolean stacking(SpanAnnotationRequest_ImplBase<?> aRequest, AnnotationFS aSpan)
    {
        return stacking(aRequest.getBegin(), aRequest.getEnd(), aSpan.getBegin(), aSpan.getEnd());
    }

    private static boolean stacking(AnnotationFS aFS1, AnnotationFS aFS2)
    {
        return stacking(aFS1.getBegin(), aFS1.getEnd(), aFS2.getBegin(), aFS2.getEnd());
    }

    private static boolean stacking(int aBegin1, int aEnd1, int aBegin2, int aEnd2)
    {
        return aBegin1 == aBegin2 && aEnd1 == aEnd2;
    }
}
