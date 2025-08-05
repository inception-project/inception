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

import static de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter.SpanOption.TRIM;

import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.DeleteSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class SegmentationUnitAdapter
{
    private static final Set<String> SEGMENTATION_TYPES = Set.of(Token._TypeName,
            Sentence._TypeName);

    private final SpanAdapterImpl spanAdapter;

    public boolean accepts(String aTypeName)
    {
        return SEGMENTATION_TYPES.contains(aTypeName);
    }

    public SegmentationUnitAdapter(SpanAdapter aSpanAdapter)
    {
        spanAdapter = (SpanAdapterImpl) aSpanAdapter;
    }

    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (aRequest.getBegin() != aRequest.getEnd()) {
            throw new IllegalPlacementException(
                    "Can only split unit, not create an entirely new one.");
        }

        if (Token._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            return splitUnit(aRequest, Token.class);
        }

        if (Sentence._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            var cas = aRequest.getCas();
            var tokensToBeSplit = cas.select(Token.class) //
                    .covering(aRequest.getBegin(), aRequest.getBegin()) //
                    .filter(t -> t.getBegin() != aRequest.getBegin()
                            && aRequest.getBegin() != t.getEnd())
                    .toList();

            if (!tokensToBeSplit.isEmpty()) {
                throw new IllegalPlacementException(
                        "Sentences can only be split at token boundaries.");
            }

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
                    (Annotation) aRequest.getAnnotation());
            return;
        }

        if (Sentence._TypeName.equals(spanAdapter.getAnnotationTypeName())) {
            deleteAndMergeUnit(aRequest.getDocument(), aRequest.getDocumentOwner(),
                    (Annotation) aRequest.getAnnotation());
            return;
        }

        throw new IllegalPlacementException(
                "Annotation type not supported: " + spanAdapter.getAnnotationTypeName());
    }

    public <T extends Annotation> void deleteAndMergeUnit(SourceDocument aDocument,
            String aDocumentOwner, Annotation aUnit)
        throws AnnotationException
    {
        var cas = aUnit.getCAS();

        // First try to merge with the preceding unit
        var precedingUnit = cas.<Annotation> select(aUnit.getType()).preceding(aUnit).limit(1)
                .singleOrNull();

        if (!spanAdapter.getLayer().isCrossSentence() && !sameSentence(aUnit, precedingUnit)) {
            precedingUnit = null;
        }

        if (precedingUnit != null) {
            var oldBegin = precedingUnit.getBegin();
            var oldEnd = precedingUnit.getEnd();
            var newBegin = precedingUnit.getBegin();
            int newEnd = aUnit.getEnd();
            deleteAndMerge(aDocument, aDocumentOwner, cas, precedingUnit, aUnit, oldBegin, oldEnd,
                    newBegin, newEnd);
            return;
        }

        // Then try to merge with the following unit
        var followingUnit = cas.<Annotation> select(aUnit.getType()).following(aUnit).limit(1)
                .singleOrNull();

        if (!spanAdapter.getLayer().isCrossSentence() && !sameSentence(aUnit, followingUnit)) {
            followingUnit = null;
        }

        if (followingUnit != null) {
            var oldBegin = followingUnit.getBegin();
            var oldEnd = followingUnit.getEnd();
            var newBegin = aUnit.getBegin();
            var newEnd = followingUnit.getEnd();
            deleteAndMerge(aDocument, aDocumentOwner, cas, followingUnit, aUnit, oldBegin, oldEnd,
                    newBegin, newEnd);
            return;
        }

        throw new IllegalPlacementException("The last unit cannot be deleted.");
    }

    private boolean sameSentence(Annotation a1, Annotation a2)
    {
        if (a1 == null || a2 == null) {
            return false;
        }

        var cas = a1.getCAS();
        var s1 = cas.select(Sentence.class).covering(a1).nullOK().get();
        var s2 = cas.select(Sentence.class).covering(a2).nullOK().get();

        if (s1 == null || s2 == null) {
            return false;
        }

        return s1 == s2;
    }

    private <T extends Annotation> void deleteAndMerge(SourceDocument aDocument,
            String aDocumentOwner, CAS aCas, T aAnnToBeResized, Annotation aAnnToBeRemoved,
            int oldBegin, int oldEnd, int newBegin, int newEnd)
    {
        aCas.removeFsFromIndexes(aAnnToBeRemoved);
        spanAdapter.moveSpanAnnotation(aCas, aAnnToBeResized, newBegin, newEnd, TRIM);
        spanAdapter.publishEvent(() -> new UnitMergedEvent(this, aDocument, aDocumentOwner,
                spanAdapter.getLayer(), aAnnToBeResized, oldBegin, oldEnd, aAnnToBeRemoved));
    }

    AnnotationFS handleSentenceMove(MoveSpanAnnotationRequest aRequest) throws AnnotationException
    {
        throw new IllegalPlacementException("Moving/resizing units currently not supported");
    }

    AnnotationFS handleTokenMove(MoveSpanAnnotationRequest aRequest) throws AnnotationException
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

    <T extends Annotation> AnnotationFS splitUnit(CreateSpanAnnotationRequest aRequest,
            Class<T> unitType)
        throws AnnotationException
    {
        var cas = aRequest.getCas();
        var units = cas.select(unitType) //
                .covering(aRequest.getBegin(), aRequest.getBegin()) //
                .asList();

        if (units.isEmpty()) {
            throw new IllegalPlacementException("There is no unit here to split.");
        }

        if (units.size() > 1) {
            throw new IllegalPlacementException(
                    "Splitting a unit may not create a zero-width unit.");
        }

        var unit = units.get(0);

        var oldBegin = unit.getBegin();
        var oldEnd = unit.getEnd();

        var headRange = new int[] { unit.getBegin(), aRequest.getBegin() };
        var tailRange = new int[] { aRequest.getEnd(), oldEnd };

        TrimUtils.trim(cas.getDocumentText(), headRange);
        TrimUtils.trim(cas.getDocumentText(), tailRange);

        if (headRange[0] == headRange[1] || tailRange[0] == tailRange[1]) {
            throw new IllegalPlacementException(
                    "Splitting a unit may not create a zero-width unit.");
        }

        var resizedUnit = spanAdapter.moveSpanAnnotation(cas, unit, headRange[0], headRange[1],
                TRIM);
        var newUnit = spanAdapter.createSpanAnnotation(cas, tailRange[0], tailRange[1], TRIM);
        spanAdapter.publishEvent(
                () -> new UnitSplitEvent(this, aRequest.getDocument(), aRequest.getDocumentOwner(),
                        spanAdapter.getLayer(), resizedUnit, oldBegin, oldEnd, newUnit));

        return newUnit;
    }

    public <T extends Annotation> AnnotationFS unMerge(SourceDocument aDocument,
            String aDocumentOwner, CAS aCas, VID aResizedUnit, int aBegin, int aEnd,
            VID aDeletedUnit)
        throws AnnotationException
    {
        var resizedUnit = ICasUtil.selectAnnotationByAddr(aCas, aResizedUnit.getId());
        spanAdapter.moveSpanAnnotation(aCas, resizedUnit, aBegin, aEnd, TRIM);
        return spanAdapter.restore(aDocument, aDocumentOwner, aCas, aDeletedUnit);
    }

    public <T extends Annotation> AnnotationFS unSplit(SourceDocument aDocument,
            String aDocumentOwner, CAS aCas, VID aResizedUnit, int aBegin, int aEnd, VID aNewUnit)
        throws AnnotationException
    {
        spanAdapter.delete(aDocument, aDocumentOwner, aCas, aNewUnit);
        var resizedUnit = ICasUtil.selectAnnotationByAddr(aCas, aResizedUnit.getId());
        return spanAdapter.moveSpanAnnotation(aCas, resizedUnit, aBegin, aEnd, TRIM);
    }
}
