/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.io.Serializable;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Selection
    implements Serializable
{
    private static final Logger LOG = LoggerFactory.getLogger(Selection.class);
    
    private static final long serialVersionUID = 2257223261821341371L;

    // is the annotation span or arc annotation
    private boolean arc;

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
    
    public void selectArc(VID aVid, AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        arc = true;
        selectedAnnotationId = aVid;
        text = "[" + aOriginFs.getCoveredText() + "] - [" + aTargetFs.getCoveredText() + "]";
        beginOffset = Math.min(aOriginFs.getBegin(), aTargetFs.getBegin());
        endOffset = Math.max(aOriginFs.getEnd(), aTargetFs.getEnd());
        
        // Properties used when an arc is selected
        originSpanId = getAddr(aOriginFs);
        targetSpanId = getAddr(aTargetFs);
        
        LOG.debug("Arc: {}", this);
    }

    public void selectSpan(AnnotationFS aFS)
    {
        selectSpan(new VID(aFS), aFS.getCAS(), aFS.getBegin(), aFS.getEnd());
    }
    
    public void selectSpan(VID aVid, CAS aCAS, int aBegin, int aEnd)
    {
        arc = false;
        selectedAnnotationId = aVid;
        text = aCAS.getDocumentText().substring(aBegin, aEnd);
        beginOffset = aBegin;
        endOffset = aEnd;
        
        // Properties used when an arc is selected
        originSpanId = -1;
        targetSpanId = -1;
        
        
        LOG.debug("Span: {}", this);
    }

    public void selectSpan(JCas aJCas, int aBegin, int aEnd)
    {
        selectSpan(VID.NONE_ID, aJCas.getCas(), aBegin, aEnd);
    }
    
    public void clear()
    {
        arc = false;
        selectedAnnotationId = VID.NONE_ID;
        beginOffset = -1;
        endOffset = -1;
        text = "";
        
        // Properties used when an arc is selected
        originSpanId = -1;
        targetSpanId = -1;
        
        LOG.debug("Clear: {}", this);
    }

    public boolean isSpan()
    {
        return !arc;
    }

    public boolean isArc()
    {
        return arc;
    }

    public int getOrigin()
    {
        if (!arc) {
            throw new IllegalStateException("Selected annotation is not an arc");
        }
        
        return originSpanId;
    }

    public int getTarget()
    {
        if (!arc) {
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

    public void setText(String aText)
    {
        text = aText;
    }

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
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Selection [");
        if (arc) {
            builder.append("arc");
        }
        else {
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
        sel.arc = arc;
        sel.originSpanId = originSpanId;
        sel.targetSpanId = targetSpanId;
        sel.beginOffset = beginOffset;
        sel.endOffset = endOffset;
        sel.selectedAnnotationId = selectedAnnotationId;
        sel.text = text;        
        return sel;
    }
    
    public void set(Selection aSelection)
    {
        arc = aSelection.arc;
        originSpanId = aSelection.originSpanId;
        targetSpanId = aSelection.targetSpanId;
        beginOffset = aSelection.beginOffset;
        endOffset = aSelection.endOffset;
        selectedAnnotationId = aSelection.selectedAnnotationId;
        text = aSelection.text;        
    }
}
