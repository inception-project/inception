/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.command;

import java.io.Serializable;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;

public class Selection
    implements Serializable
{
    private static final long serialVersionUID = 2257223261821341371L;
    
    // if it is annotation or delete operation
    private boolean isAnnotate;

    // is the annotation span or arc annotation
    private boolean isRelationAnno;
    
    // the span id of the dependent in arc annotation
    private int originSpanId;

    // The type of the dependent in the arc annotation
    private String originSpanType;

    // The type of the governor in the arc annotation
    private String targetSpanType;

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

    public boolean isRelationAnno()
    {
        return isRelationAnno;
    }

    public void setRelationAnno(boolean isRelationAnno)
    {
        this.isRelationAnno = isRelationAnno;
    }

    public boolean isAnnotate()
    {
        return isAnnotate;
    }

    public void setAnnotate(boolean isAnnotate)
    {
        this.isAnnotate = isAnnotate;
    }
    
    public int getOriginSpanId()
    {
        return originSpanId;
    }

    public void setOriginSpanId(int originSpanId)
    {
        this.originSpanId = originSpanId;
    }

    public String getOriginSpanType()
    {
        return originSpanType;
    }

    public void setOriginSpanType(String originSpanType)
    {
        this.originSpanType = originSpanType;
    }

    public String getTargetSpanType()
    {
        return targetSpanType;
    }

    public void setTargetSpanType(String targetSpanType)
    {
        this.targetSpanType = targetSpanType;
    }

    public int getTargetSpanId()
    {
        return targetSpanId;
    }

    public void setTargetSpanId(int targetSpanId)
    {
        this.targetSpanId = targetSpanId;
    }

    public int getBegin()
    {
        return beginOffset;
    }

    public void setBegin(int beginOffset)
    {
        this.beginOffset = beginOffset;
    }

    public int getEnd()
    {
        return endOffset;
    }

    public void setEnd(int endOffset)
    {
        this.endOffset = endOffset;
    }
    
    public String getText()
    {
        return text;
    }

    public void setText(String selectedText)
    {
        this.text = selectedText;
    }
    
    public VID getSelectedAnnotationId()
    {
        return selectedAnnotationId;
    }

    public void setSelectedAnnotationId(VID selectedAnnotationId)
    {
        this.selectedAnnotationId = selectedAnnotationId;
    }
    
    public void clear()
    {
        setText("");
        setSelectedAnnotationId(VID.NONE_ID);
        // 
        // getCommand().setBeginOffset(-1);
        // getCommand().setEndOffset(-1);
    }

    public void set(JCas aJCas, int aBegin, int aEnd)
    {
        setBegin(aBegin);
        setEnd(aEnd);
        setText(aJCas.getDocumentText().substring(aBegin, aEnd));
    }
}
