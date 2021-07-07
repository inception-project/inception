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

public class ClientMessage
{
    private String username;
    private String clientName;
    private long project;
    private long document;
    private int[][] viewport;
    private int annotationAddress;
    private String annotationType;
    private String annotationFeature;
    private int annotationOffsetBegin;
    private int annotationOffsetEnd;
    private String offsetType;


    public ClientMessage()
    {
        //Default
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public long getProject()
    {
        return project;
    }

    public void setProject(long project)
    {
        this.project = project;
    }

    public long getDocument()
    {
        return document;
    }

    public void setDocument(long document)
    {
        this.document = document;
    }

    public int[][] getViewport()
    {
        return viewport;
    }

    public void setViewport(int[][] viewport)
    {
        this.viewport = viewport;
    }

    public int getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(int annotationAddress)
    {
        this.annotationAddress = annotationAddress;
    }

    public String getAnnotationType()
    {
        return annotationType;
    }

    public void setAnnotationType(String annotationType)
    {
        this.annotationType = annotationType;
    }

    public int getAnnotationOffsetBegin()
    {
        return annotationOffsetBegin;
    }

    public void setAnnotationOffsetBegin(int aAnnotationOffsetBegin)
    {
        annotationOffsetBegin = aAnnotationOffsetBegin;
    }

    public int getAnnotationOffsetEnd()
    {
        return annotationOffsetEnd;
    }

    public void setAnnotationOffsetEnd(int aAannotationOffsetEnd)
    {
        annotationOffsetEnd = aAannotationOffsetEnd;
    }

    public String getOffsetType()
    {
        return offsetType;
    }

    public void setOffsetType(String aOffsetType)
    {
        offsetType = aOffsetType;
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
