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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil.getAddr;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.Serializable;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class Selection
    implements Serializable
{
    private static final Logger LOG = LoggerFactory.getLogger(Selection.class);

    private static final long serialVersionUID = 2257223261821341371L;

    // the span id of the dependent in arc annotation
    private int originSpanId;

    // The span id of the governor in arc annotation
    private int targetSpanId;

    // the begin offset of a span annotation
    private int beginOffset;

    // the end offset of a span annotation
    private int endOffset;

    // id of the select annotation layer
    private VID selectedAnnotationId = VID.NONE_ID;

    // selected span text
    private String text;

    private String originText;
    private String targetText;

    public Selection()
    {
        // Nothing to do
    }

    public void selectArc(AnnotationFS aFS)
    {
        Type depType = aFS.getType();
        Feature originFeat = depType.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeat = depType.getFeatureByBaseName(FEAT_REL_TARGET);
        AnnotationFS originFS = (AnnotationFS) aFS.getFeatureValue(originFeat);
        AnnotationFS targetFS = (AnnotationFS) aFS.getFeatureValue(targetFeat);
        selectArc(new VID(aFS), originFS, targetFS);
    }

    public void selectArc(AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        selectArc(VID.NONE_ID, aOriginFs, aTargetFs);
    }

    public void selectArc(VID aVid, AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        selectedAnnotationId = aVid;
        text = "[" + aOriginFs.getCoveredText() + "] - [" + aTargetFs.getCoveredText() + "]";
        originText = aOriginFs.getCoveredText();
        targetText = aTargetFs.getCoveredText();
        beginOffset = Math.min(aOriginFs.getBegin(), aTargetFs.getBegin());
        endOffset = Math.max(aOriginFs.getEnd(), aTargetFs.getEnd());

        // Properties used when an arc is selected
        originSpanId = getAddr(aOriginFs);
        targetSpanId = getAddr(aTargetFs);

        LOG.debug("Arc: {}", this);

        fireSelectionChanged();
    }

    public void selectSpan(AnnotationFS aFS)
    {
        selectSpan(new VID(aFS), aFS.getCAS(), aFS.getBegin(), aFS.getEnd());
    }

    public void selectSpan(VID aVid, CAS aCAS, int aBegin, int aEnd)
    {
        selectedAnnotationId = aVid;
        text = aCAS.getDocumentText().substring(aBegin, aEnd);
        beginOffset = aBegin;
        endOffset = aEnd;

        // Properties used when an arc is selected
        originSpanId = -1;
        targetSpanId = -1;
        originText = null;
        targetText = null;

        LOG.debug("Span: {}", this);

        fireSelectionChanged();
    }

    public void selectSpan(CAS aCas, int aBegin, int aEnd)
    {
        selectSpan(VID.NONE_ID, aCas, aBegin, aEnd);
    }

    public void clear()
    {
        selectedAnnotationId = VID.NONE_ID;
        beginOffset = -1;
        endOffset = -1;
        text = "";

        // Properties used when an arc is selected
        originSpanId = -1;
        targetSpanId = -1;

        LOG.debug("Clear: {}", this);

        fireSelectionChanged();
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
     * @deprecated Should no longer be used. Instead, text is set implicitly through
     *             {@link #selectSpan} and {@code #selectArc}.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public void setText(String aText)
    {
        text = aText;
    }

    /**
     * If an existing annotation is selected, it is returned here. Mind that a selection does not
     * have to point at an existing annotation. It can also point just at a span of text or at the
     * endpoints of a relation. In this case, the enpoints or begin/end offsets are set, but not the
     * annotation ID.
     * 
     * @return the VID;
     */
    public VID getAnnotation()
    {
        return selectedAnnotationId;
    }

    public void setAnnotation(VID aVID)
    {
        selectedAnnotationId = aVID;
    }

    public void reverseArc()
    {
        int tempSpanId = originSpanId;
        originSpanId = targetSpanId;
        targetSpanId = tempSpanId;

        String tempText = originText;
        originText = targetText;
        targetText = tempText;

        fireSelectionChanged();
    }

    private void fireSelectionChanged()
    {
        Optional<IPageRequestHandler> handler = RequestCycle.get().find(IPageRequestHandler.class);
        if (handler.isPresent() && handler.get().isPageInstanceCreated()) {
            Page page = (Page) handler.get().getPage();
            page.send(page, BREADTH, new SelectionChangedEvent(
                    RequestCycle.get().find(AjaxRequestTarget.class).orElse(null)));
        }
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

    public Selection copy()
    {
        Selection sel = new Selection();
        sel.originSpanId = originSpanId;
        sel.targetSpanId = targetSpanId;
        sel.beginOffset = beginOffset;
        sel.endOffset = endOffset;
        sel.selectedAnnotationId = selectedAnnotationId;
        sel.text = text;
        sel.originText = originText;
        sel.targetText = targetText;
        return sel;
    }

    public void set(Selection aSelection)
    {
        originSpanId = aSelection.originSpanId;
        targetSpanId = aSelection.targetSpanId;
        beginOffset = aSelection.beginOffset;
        endOffset = aSelection.endOffset;
        selectedAnnotationId = aSelection.selectedAnnotationId;
        text = aSelection.text;
        originText = aSelection.originText;
        targetText = aSelection.targetText;
    }
}
