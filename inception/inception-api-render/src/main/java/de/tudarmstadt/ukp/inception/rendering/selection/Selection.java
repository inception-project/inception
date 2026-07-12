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
package de.tudarmstadt.ukp.inception.rendering.selection;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.Range.rangeClippedToDocument;

import java.io.Serializable;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.Range;

/**
 * An immutable snapshot of what an editor has selected. Instances are created through the static
 * factory methods ({@link #unselected()}, {@link #span}, {@link #arc}); the current selection of an
 * editor is replaced wholesale via {@code AnnotatorState.setSelection(Selection)}, which is the
 * single place that broadcasts a {@link SelectionChangedEvent}.
 */
public class Selection
    implements Serializable
{
    private static final Logger LOG = LoggerFactory.getLogger(Selection.class);

    private static final long serialVersionUID = 2257223261821341371L;

    private static final Selection UNSELECTED = new Selection(VID.NONE_ID, -1, -1, "", -1, -1, null,
            null);

    // the span id of the dependent in arc annotation
    private final int originSpanId;

    // The span id of the governor in arc annotation
    private final int targetSpanId;

    // the begin offset of a span annotation
    private final int beginOffset;

    // the end offset of a span annotation
    private final int endOffset;

    // id of the select annotation layer
    private final VID selectedAnnotationId;

    // selected span text
    private final String text;

    private final String originText;
    private final String targetText;

    private Selection(VID aSelectedAnnotationId, int aBeginOffset, int aEndOffset, String aText,
            int aOriginSpanId, int aTargetSpanId, String aOriginText, String aTargetText)
    {
        selectedAnnotationId = aSelectedAnnotationId;
        beginOffset = aBeginOffset;
        endOffset = aEndOffset;
        text = aText;
        originSpanId = aOriginSpanId;
        targetSpanId = aTargetSpanId;
        originText = aOriginText;
        targetText = aTargetText;
    }

    /**
     * @return the empty selection (nothing selected).
     */
    public static Selection unselected()
    {
        return UNSELECTED;
    }

    public static Selection arc(AnnotationFS aFS)
    {
        var depType = aFS.getType();
        var originFeat = depType.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeat = depType.getFeatureByBaseName(FEAT_REL_TARGET);
        var originFS = (AnnotationFS) aFS.getFeatureValue(originFeat);
        var targetFS = (AnnotationFS) aFS.getFeatureValue(targetFeat);
        return arc(VID.of(aFS), originFS, targetFS);
    }

    public static Selection arc(AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        return arc(VID.NONE_ID, aOriginFs, aTargetFs);
    }

    public static Selection arc(VID aVid, AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        if (aVid.isSet()) {
            try {
                ICasUtil.selectAnnotationByAddr(aOriginFs.getCAS(), aVid.getId());
            }
            catch (Exception e) {
                LOG.error("While selecting an arc the VID does not point to a valid annotation", e);
                return unselected();
            }
        }

        var clippedRange = rangeClippedToDocument(aOriginFs.getCAS(),
                Math.min(aOriginFs.getBegin(), aTargetFs.getBegin()),
                Math.max(aOriginFs.getEnd(), aTargetFs.getEnd()));

        var selection = new Selection(aVid, clippedRange.getBegin(), clippedRange.getEnd(),
                "[" + aOriginFs.getCoveredText() + "] - [" + aTargetFs.getCoveredText() + "]",
                getAddr(aOriginFs), getAddr(aTargetFs), aOriginFs.getCoveredText(),
                aTargetFs.getCoveredText());

        LOG.trace("Arc selected: {}", selection);

        return selection;
    }

    public static Selection span(AnnotationFS aFS)
    {
        return span(VID.of(aFS), aFS.getCAS(), aFS.getBegin(), aFS.getEnd());
    }

    public static Selection span(CAS aCas, int aBegin, int aEnd)
    {
        return span(VID.NONE_ID, aCas, aBegin, aEnd);
    }

    public static Selection span(VID aVid, CAS aCAS, int aBegin, int aEnd)
    {
        if (aVid.isSet()) {
            try {
                ICasUtil.selectAnnotationByAddr(aCAS, aVid.getId());
            }
            catch (Exception e) {
                LOG.error("While selecting a span the VID does not point to a valid annotation", e);
                return unselected();
            }
        }

        var clippedRange = Range.rangeClippedToDocument(aCAS, aBegin, aEnd);

        // Properties used when an arc is selected (origin/target) are cleared for a span
        var selection = new Selection(aVid, clippedRange.getBegin(), clippedRange.getEnd(),
                aCAS.getDocumentText().substring(aBegin, aEnd), -1, -1, null, null);

        LOG.trace("Span selected: {}", selection);

        return selection;
    }

    public boolean isSet()
    {
        return isSpan() || isArc();
    }

    public boolean isSpan()
    {
        return originSpanId == -1 && targetSpanId == -1 && beginOffset != -1 && endOffset != -1;
    }

    public boolean isArc()
    {
        return originSpanId != -1 && targetSpanId != -1;
    }

    public int getOrigin()
    {
        if (!isArc()) {
            throw new IllegalStateException("Selected annotation is not an arc");
        }

        return originSpanId;
    }

    public int getTarget()
    {
        if (!isArc()) {
            throw new IllegalStateException("Selected annotation is not an arc");
        }

        return targetSpanId;
    }

    public int getBegin()
    {
        return beginOffset;
    }

    public int getEnd()
    {
        return endOffset;
    }

    public String getText()
    {
        return text;
    }

    public String getOriginText()
    {
        return originText;
    }

    public String getTargetText()
    {
        return targetText;
    }

    /**
     * If an existing annotation is selected, it is returned here. Mind that a selection does not
     * have to point at an existing annotation. It can also point just at a span of text or at the
     * endpoints of a relation. In this case, the endpoints or begin/end offsets are set, but not
     * the annotation ID.
     *
     * @return the VID;
     */
    public VID getAnnotation()
    {
        return selectedAnnotationId;
    }

    @Override
    public boolean equals(Object aObj)
    {
        if (this == aObj) {
            return true;
        }
        if (!(aObj instanceof Selection)) {
            return false;
        }
        var other = (Selection) aObj;
        return originSpanId == other.originSpanId && targetSpanId == other.targetSpanId
                && beginOffset == other.beginOffset && endOffset == other.endOffset
                && Objects.equals(selectedAnnotationId, other.selectedAnnotationId)
                && Objects.equals(text, other.text) && Objects.equals(originText, other.originText)
                && Objects.equals(targetText, other.targetText);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originSpanId, targetSpanId, beginOffset, endOffset,
                selectedAnnotationId, text, originText, targetText);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Selection [");
        if (!isSet()) {
            builder.append("UNSET");
        }
        if (isArc()) {
            builder.append("arc");
        }
        if (isSpan()) {
            builder.append("span");
        }
        builder.append(", origin=");
        builder.append(originSpanId);
        builder.append(", target=");
        builder.append(targetSpanId);
        builder.append(", begin=");
        builder.append(beginOffset);
        builder.append(", end=");
        builder.append(endOffset);
        builder.append(", vid=");
        builder.append(selectedAnnotationId);
        builder.append(", text=[");
        builder.append(text);
        builder.append("]]");
        return builder.toString();
    }
}
