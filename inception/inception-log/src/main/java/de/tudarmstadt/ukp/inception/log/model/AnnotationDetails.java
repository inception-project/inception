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
package de.tudarmstadt.ukp.inception.log.model;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

@JsonInclude(Include.NON_NULL)
public class AnnotationDetails
{
    private int addr = -1;
    private int begin;
    private int end;
    private String type;
    private String text;

    public AnnotationDetails()
    {
        // Nothing to do
    }

    public AnnotationDetails(FeatureStructure aFS)
    {
        addr = ICasUtil.getAddr(aFS);
        type = aFS.getType().getName();
        if (aFS instanceof AnnotationFS) {
            AnnotationFS annoFS = (AnnotationFS) aFS;
            begin = annoFS.getBegin();
            end = annoFS.getEnd();
            text = annoFS.getCoveredText();
        }
        else {
            begin = -1;
            end = -1;
            text = null;
        }
    }

    public int getAddr()
    {
        return addr;
    }

    public void setAddr(int aAddr)
    {
        addr = aAddr;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int aBegin)
    {
        begin = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }
}
