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
package de.tudarmstadt.ukp.inception.experimental.api.messages.request;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * DeleteAnnotationRequest: Request from Client to delete a certain annotation
 *
 * Attributes:
 * annotatorName: String representation of the name of the annotator the annotation will belong to
 * projectId: The ID of the project the annotation will belong to
 * sourceDocumentId: The ID of the sourcedocument the annotation belongs to
 * annotationId: The ID of the annotation to delete
 * layerId: The ID of the layer the annotation belongs to
 **/
public class DeleteAnnotationRequest
{
    private String annotatorName;
    private long projectId;
    private long sourceDocumentId;
    private VID annotationId;
    private long layerId;

    public String getAnnotatorName()
    {
        return annotatorName;
    }

    public void setAnnotatorName(String aAnnotatorName)
    {
        annotatorName = aAnnotatorName;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long aProjectId)
    {
        projectId = aProjectId;
    }

    public long getSourceDocumentId()
    {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(long aSourceDocumentId)
    {
        sourceDocumentId = aSourceDocumentId;
    }

    public VID getAnnotationId()
    {
        return annotationId;
    }

    public void setAnnotationId(VID aAnnotationId)
    {
        annotationId = aAnnotationId;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long aLayerId)
    {
        layerId = aLayerId;
    }
}
