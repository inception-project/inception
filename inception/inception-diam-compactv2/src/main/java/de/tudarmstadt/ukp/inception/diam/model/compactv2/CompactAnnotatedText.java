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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;

public class CompactAnnotatedText
    extends AjaxResponse
{
    public static final String COMMAND = "getAnnotatedDocument";

    private List<CompactLayer> layers = new ArrayList<>();

    private @JsonInclude(NON_NULL) String text;
    private CompactRange window;

    private @JsonInclude(NON_EMPTY) List<CompactRelation> relations = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<CompactSpan> spans = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<CompactAnnotationMarker> annotationMarkers = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<CompactTextMarker> textMarkers = new ArrayList<>();

    public CompactAnnotatedText()
    {
        super(COMMAND);
    }

    public void setLayers(List<CompactLayer> aLayers)
    {
        layers = aLayers;
    }

    public List<CompactLayer> getLayers()
    {
        return layers;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public void setWindow(CompactRange aWindow)
    {
        window = aWindow;
    }

    public CompactRange getWindow()
    {
        return window;
    }

    public List<CompactRelation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<CompactRelation> aRelations)
    {
        relations = aRelations;
    }

    public void addRelation(CompactRelation aRelation)
    {
        relations.add(aRelation);
    }

    public List<CompactSpan> getSpans()
    {
        return spans;
    }

    public void setSpans(List<CompactSpan> aEntities)
    {
        spans = aEntities;
    }

    public void addSpan(CompactSpan aEntity)
    {
        spans.add(aEntity);
    }

    public List<CompactAnnotationMarker> getAnnotationMarkers()
    {
        return annotationMarkers;
    }

    public void setAnnotationMarkers(List<CompactAnnotationMarker> aMarkers)
    {
        annotationMarkers = aMarkers;
    }

    public void addAnnotationMarker(CompactAnnotationMarker aMarker)
    {
        annotationMarkers.add(aMarker);
    }

    public List<CompactTextMarker> getTextMarkers()
    {
        return textMarkers;
    }

    public void setTextMarkers(List<CompactTextMarker> aTextMarkers)
    {
        textMarkers = aTextMarkers;
    }

    public void addTextMarker(CompactTextMarker aMarker)
    {
        textMarkers.add(aMarker);
    }
}
