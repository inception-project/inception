/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import java.io.Serializable;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;

/**
 * A Model comprises of document and collection brat responses togehter with the username that will
 * populate the sentence with {@link AnnotationDocument}s
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
 *
 */
public class CurationUserSegmentForAnnotationDocument
    implements Serializable
{

    private static final long serialVersionUID = 1785666148278992450L;
    private String documentResponse;
    private String collectionData = "{}";
    private String username = "";
    //NEw Additions
    private BratAnnotatorModel bratAnnotatorModel;
    private  AnnotationSelection annotationSelection;
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress;

    public String getDocumentResponse()
    {
        return documentResponse;
    }

    public void setDocumentResponse(String documentResponse)
    {
        this.documentResponse = documentResponse;
    }

    public String getCollectionData()
    {
        return collectionData;
    }

    public void setCollectionData(String collectionData)
    {
        this.collectionData = collectionData;
    }

    public boolean equals(CurationUserSegmentForAnnotationDocument segment)
    {
        return segment.getCollectionData().equals(collectionData)
                && segment.getDocumentResponse().equals(documentResponse);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String aUsername)
    {
        username = aUsername;
    }

    public BratAnnotatorModel getBratAnnotatorModel()
    {
        return bratAnnotatorModel;
    }

    public void setBratAnnotatorModel(BratAnnotatorModel bratAnnotatorModel)
    {
        this.bratAnnotatorModel = bratAnnotatorModel;
    }

    public AnnotationSelection getAnnotationSelection()
    {
        return annotationSelection;
    }

    public void setAnnotationSelection(AnnotationSelection annotationSelection)
    {
        this.annotationSelection = annotationSelection;
    }

    public Map<String, Map<Integer, AnnotationSelection>> getAnnotationSelectionByUsernameAndAddress()
    {
        return annotationSelectionByUsernameAndAddress;
    }

    public void setAnnotationSelectionByUsernameAndAddress(
            Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress)
    {
        this.annotationSelectionByUsernameAndAddress = annotationSelectionByUsernameAndAddress;
    }


}
