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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter.SpanOption.TRIM;

import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public class SegmentationUnitAdapter
{
    private static final Set<String> SEGMENTATION_TYPES = Set.of(Token._TypeName,
            Sentence._TypeName);

    private final SpanAdapter spanAdapter;

    public boolean accepts(String aTypeName)
    {
        return SEGMENTATION_TYPES.contains(aTypeName);
    }

    public SegmentationUnitAdapter(SpanAdapter aSpanAdapter)
    {
        spanAdapter = aSpanAdapter;
    }

    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (Token._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            return splitUnit(aRequest, Token.class);
        }

        if (Sentence._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            return splitUnit(aRequest, Sentence.class);
        }

        throw new IllegalPlacementException(
                "Annotation type not supported: " + spanAdapter.getAnnotationTypeName());
    }

    public AnnotationFS handle(MoveSpanAnnotationRequest aRequest) throws AnnotationException
    {
        var ann = aRequest.getAnnotation();
        if (aRequest.getBegin() == ann.getBegin() && aRequest.getEnd() == ann.getEnd()) {
            // NOP
            return ann;
        }

        if (Token._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            return handleTokenMove(aRequest);
        }

        if (Sentence._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            return handleSentenceMove(aRequest);
        }

        throw new IllegalPlacementException(
                "Annotation type not supported: " + spanAdapter.getAnnotationTypeName());
    }

    public void handle(DeleteSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (Token._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            deleteAndMergeUnit(aRequest.getDocument(), aRequest.getDocumentOwner(),
                    aRequest.getCas(), (Annotation) aRequest.getAnnotation(), Token.class);
            return;
        }

        if (Sentence._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            deleteAndMergeUnit(aRequest.getDocument(), aRequest.getDocumentOwner(),
                    aRequest.getCas(), (Annotation) aRequest.getAnnotation(), Sentence.class);
            return;
        }

        throw new IllegalPlacementException(
                "Annotation type not supported: " + spanAdapter.getAnnotationTypeName());
    }

    private <T extends Annotation> void deleteAndMergeUnit(SourceDocument aDocument,
            String aDocumentOwner, CAS aCas, Annotation aUnit, Class<T> aClass)
        throws AnnotationException
    {
        // First try to merge with the preceding unit
        var precedingUnit = aCas.select(aClass).preceding(aUnit).limit(1).singleOrNull();
        if (precedingUnit != null) {
            var oldBegin = precedingUnit.getBegin();
            var oldEnd = precedingUnit.getEnd();
            spanAdapter.moveSpanAnnotation(aCas, precedingUnit, precedingUnit.getBegin(),
                    aUnit.getEnd(), TRIM);
            spanAdapter.publishEvent(() -> new SpanMovedEvent(this, aDocument, aDocumentOwner,
                    spanAdapter.getLayer(), precedingUnit, oldBegin, oldEnd));
            return;
        }

        // Then try to merge with the following unit
        var followingUnit = aCas.select(aClass).preceding(aUnit).limit(1).singleOrNull();
        if (followingUnit != null) {
            var oldBegin = followingUnit.getBegin();
            var oldEnd = followingUnit.getEnd();
            spanAdapter.moveSpanAnnotation(aCas, followingUnit, aUnit.getBegin(),
                    followingUnit.getEnd(), TRIM);
            spanAdapter.publishEvent(() -> new SpanMovedEvent(this, aDocument, aDocumentOwner,
                    spanAdapter.getLayer(), followingUnit, oldBegin, oldEnd));
            return;
        }

        throw new IllegalPlacementException("The last unit cannot be deleted.");
    }

    private AnnotationFS handleSentenceMove(MoveSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        throw new IllegalPlacementException("Moving/resizing units currently not supported");
    }

    private AnnotationFS handleTokenMove(MoveSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        throw new IllegalPlacementException("Moving/resizing units currently not supported");

        // var cas = aRequest.getCas();
        // var ann = (Annotation) aRequest.getAnnotation();
        //
        // if (aRequest.getBegin() != ann.getBegin() && aRequest.getEnd() != ann.getEnd()) {
        // throw new IllegalPlacementException(
        // "Can only resize at start or at end. Cannot move or resize at both ends at the same
        // time.");
        // }
        //
        // if (aRequest.getBegin() != ann.getBegin()) {
        // // Expand at begin
        // if (aRequest.getBegin() < ann.getBegin()) {
        //
        // }
        //
        // // Reduce at begin
        // if (aRequest.getBegin() > ann.getBegin()) {
        //
        // }
        // }
        //
        // if (aRequest.getEnd() != ann.getEnd()) {
        // // Expand at end
        // if (aRequest.getEnd() > ann.getEnd()) {
        //
        // }
        //
        // // Reduce at end
        // if (aRequest.getEnd() < ann.getEnd()) {
        // var followingToken = cas.select(Token.class).following(ann).singleOrNull();
        // if (followingToken == null) {
        // // We make the last token smaller, so we need to add a new last token in order
        // // to keep the entire text covered in tokens.
        // }
        // else {
        // // We have a following token that we can enlarge
        // }
        // }
        // }
        //
        // return ann;
    }

    private <T extends Annotation> AnnotationFS splitUnit(CreateSpanAnnotationRequest aRequest,
            Class<T> unitType)
        throws AnnotationException
    {
        if (aRequest.getBegin() != aRequest.getEnd()) {
            throw new IllegalPlacementException(
                    "Can only split unit, not create an entirely new one.");
        }

        var cas = aRequest.getCas();
        var unit = cas.select(unitType) //
                .covering(aRequest.getBegin(), aRequest.getBegin()) //
                .singleOrNull();

        var oldBegin = unit.getBegin();
        var oldEnd = unit.getEnd();
        var head = spanAdapter.moveSpanAnnotation(cas, unit, unit.getBegin(), aRequest.getBegin(),
                TRIM);
        spanAdapter.publishEvent(() -> new SpanMovedEvent(this, aRequest.getDocument(),
                aRequest.getDocumentOwner(), spanAdapter.getLayer(), head, oldBegin, oldEnd));

        var tail = spanAdapter.createSpanAnnotation(cas, aRequest.getEnd(), oldEnd, TRIM);
        spanAdapter.publishEvent(() -> new SpanCreatedEvent(this, aRequest.getDocument(),
                aRequest.getDocumentOwner(), spanAdapter.getLayer(), tail));
        return tail;
    }
}
