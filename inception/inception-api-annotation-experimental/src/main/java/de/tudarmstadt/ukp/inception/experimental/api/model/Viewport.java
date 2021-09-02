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
package de.tudarmstadt.ukp.inception.experimental.api.model;

import java.util.List;

public class Viewport
{
    private String documentText;
    private int begin;
    private int end;
    private List<Long> layers;

    public Viewport()
    {
    }

    public Viewport(String aDocumentText, int aBegin, int aEnd, List<Long> aLayers)
    {
        documentText = aDocumentText;
        begin = aBegin;
        end = aEnd;
        layers = aLayers;
    }

    public String getDocumentText()
    {
        return documentText;
    }

    public void setDocumentText(String aDocumentText)
    {
        documentText = aDocumentText;
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

    public List<Long> getLayers()
    {
        return layers;
    }

    public void setLayers(List<Long> aLayers)
    {
        layers = aLayers;
    }
}
