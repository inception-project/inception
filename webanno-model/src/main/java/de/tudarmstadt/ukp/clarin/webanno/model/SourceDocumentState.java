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
package de.tudarmstadt.ukp.clarin.webanno.model;

import de.tudarmstadt.ukp.clarin.webanno.support.PersistentEnum;

/**
 * Variables for the different states of a {@link SourceDocument} workflow.
 */
public enum SourceDocumentState
    implements PersistentEnum
{
    /**
     * No annotation document has been created for this document
     */
    NEW("NEW", "black"),

    /**
     * At least one annotation document has been created for the document
     */
    ANNOTATION_IN_PROGRESS("ANNOTATION_INPROGRESS", "black"),

    /**
     * All annotations have marked their annotation document as finished
     * 
     * @deprecated This is not used and should not be used. Will be removed in future versions. If
     *             you want to tell whether all annotators have marked a document as finished, you
     *             have to manually check if all annotators assigned to annotate this document have
     *             marked their annotation documents as done. This is nothing we can record
     *             statically in the source document.
     */
    ANNOTATION_FINISHED("ANNOTATION_FINISHED", "green"),

    /**
     * curator has started working with the annotation document, annotators can no longer make
     * modifications in annotation documents
     */
    CURATION_IN_PROGRESS("CURATION_INPROGRESS", "blue"),

    /**
     * curator claims to have curated all annotations
     */
    CURATION_FINISHED("CURATION_FINISHED", "red");

    private final String id;
    private final String color;

    SourceDocumentState(String aId, String aColor)
    {
        id = aId;
        color = aColor;
    }

    public String getName()
    {
        return getId();
    }

    @Override
    public String getId()
    {
        return id;
    }

    public String getColor()
    {
        return color;
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
