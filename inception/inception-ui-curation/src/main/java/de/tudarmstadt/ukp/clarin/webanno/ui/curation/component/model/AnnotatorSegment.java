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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * A Model comprises of document and collection brat responses together with the username that will
 * populate the sentence with {@link AnnotationDocument}s
 */
public class AnnotatorSegment
    implements Serializable
{
    private static final long serialVersionUID = 1785666148278992450L;

    private String documentResponse;
    private String collectionData = "{}";
    private User user;
    private AnnotatorState state;

    public AnnotatorSegment()
    {
        // Nothing to do
    }

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

    public User getUser()
    {
        return user;
    }

    public void setUser(User aUser)
    {
        user = aUser;
    }

    public AnnotatorState getAnnotatorState()
    {
        return state;
    }

    public void setAnnotatorState(AnnotatorState aState)
    {
        state = aState;
    }

    // FIXME: Why do we even need this?
    @Deprecated
    public boolean equals(AnnotatorSegment segment)
    {
        return segment.getCollectionData().equals(collectionData)
                && segment.getDocumentResponse().equals(documentResponse);
    }
}
