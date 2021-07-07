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
package de.tudarmstadt.ukp.inception.experimental.api.message;

public class AnnotationMessage
{
    private String annotationAddress;
    private int annotationOffsetBegin;
    private int annotationOffsetEnd;
    private String quote;
    private String color;
    private String annotationType;
    private String annotationFeature;
    private String annotationText;
    private boolean delete;
    private boolean edit;

    public AnnotationMessage()
    {
        // DEFAULT
    }

    public AnnotationMessage(String aAnnotationAddress, int aAnnotationOffsetBegin,
            int aAnnotationOffsetEnd, String aAnnotationType, String aAnnotationText)
    {
        annotationAddress = aAnnotationAddress;
        annotationOffsetBegin = aAnnotationOffsetBegin;
        annotationOffsetEnd = aAnnotationOffsetEnd;
        annotationType = aAnnotationType;
        annotationText = aAnnotationText;
    }

    public String getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(String annotationAddress)
    {
        this.annotationAddress = annotationAddress;
    }

    public int getAnnotationOffsetBegin()
    {
        return annotationOffsetBegin;
    }

    public void setAnnotationOffsetBegin(int annotationOffsetBegin)
    {
        this.annotationOffsetBegin = annotationOffsetBegin;
    }

    public int getAnnotationOffsetEnd()
    {
        return annotationOffsetEnd;
    }

    public void setAnnotationOffsetEnd(int annotationOffsetEnd)
    {
        this.annotationOffsetEnd = annotationOffsetEnd;
    }

    public String getAnnotationType()
    {
        return annotationType;
    }

    public void setAnnotationType(String annotationType)
    {
        this.annotationType = annotationType;
    }

    public String getAnnotationText()
    {
        return annotationText;
    }

    public void setAnnotationText(String annotationText)
    {
        this.annotationText = annotationText;
    }

    public String getQuote()
    {
        return quote;
    }

    public void setQuote(String aQuote)
    {
        quote = aQuote;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public boolean isDelete()
    {
        return delete;
    }

    public void setDelete(boolean aDelete)
    {
        this.delete = aDelete;
    }

    public boolean isEdit()
    {
        return edit;
    }

    public void setEdit(boolean edit)
    {
        this.edit = edit;
    }

    public String getAnnotationFeature()
    {
        return annotationFeature;
    }

    public void setAnnotationFeature(String aAnnotationFeature)
    {
        annotationFeature = aAnnotationFeature;
    }
}
