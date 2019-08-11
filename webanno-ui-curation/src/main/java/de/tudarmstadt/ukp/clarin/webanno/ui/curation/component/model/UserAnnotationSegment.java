/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import java.io.Serializable;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;

/**
 * A Model comprises of document and collection brat responses together with the username that
 * will populate the sentence with {@link AnnotationDocument}s
 */
public class UserAnnotationSegment
    implements Serializable
{
    private static final long serialVersionUID = 1785666148278992450L;
    
    private String documentResponse;
    private String collectionData = "{}";
    private String username = "";
    private AnnotatorState state;
    private AnnotationSelection annotationSelection;
    private Map<String, Map<Integer, AnnotationSelection>> selectionByUsernameAndAddress;

    public String getDocumentResponse()
    {
        return documentResponse;
    }

    public void setDocumentResponse(String aDocumentResponse)
    {
        documentResponse = aDocumentResponse;
    }

    public String getCollectionData()
    {
        return collectionData;
    }

    public void setCollectionData(String aCollectionData)
    {
        collectionData = aCollectionData;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String aUsername)
    {
        username = aUsername;
    }

    public AnnotatorState getAnnotatorState()
    {
        return state;
    }

    public void setAnnotatorState(AnnotatorState aState)
    {
        state = aState;
    }

    public AnnotationSelection getAnnotationSelection()
    {
        return annotationSelection;
    }

    public void setAnnotationSelection(AnnotationSelection aAnnotationSelection)
    {
        annotationSelection = aAnnotationSelection;
    }

    public Map<String, Map<Integer, AnnotationSelection>> getSelectionByUsernameAndAddress()
    {
        return selectionByUsernameAndAddress;
    }

    public void setSelectionByUsernameAndAddress(
            Map<String, Map<Integer, AnnotationSelection>> aSelectionByUsernameAndAddress)
    {
        selectionByUsernameAndAddress = aSelectionByUsernameAndAddress;
    }
    
    public boolean equals(UserAnnotationSegment segment)
    {
        return segment.getCollectionData().equals(collectionData)
                && segment.getDocumentResponse().equals(documentResponse);
    }
}
