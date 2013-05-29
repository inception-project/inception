/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.clarin.webanno.model;

/**
 * Variables for the different states of a {@link SourceDocument} workflow.
 *
 * @author Seid Muhie Yimam
 *
 */
public enum SourceDocumentState implements PersistentEnum
{

    /**
     * no annotation document has been created for this document
     */
    NEW("NEW"),
    /**
     * at least one annotation document has been created for the document
     */
    ANNOTATION_IN_PROGRESS("ANNOTATION_INPROGRESS"),
    /**
     * all annotations have marked their annotation document as finished
     */
    ANNOTATION_FINISHED("ANNOTATION_FINISHED"),
    /**
     * curator claims to have curated all annotations
     */
    CURATION_FINISHED("CURATION_FINISHED"),
    /**
     * curator has started working with the annotation document, annotators can no longer make
     * modifications in annotation documents
     */
    CURATION_IN_PROGRESS("CURATION_INPROGRESS");
    public String getName()
    {
        return getId();
    }
    @Override
    public String toString()
    {
        return getId();
    }
    SourceDocumentState(String aId)
    {
        this.id = aId;
    }

    private final String id;

    @Override
    public String getId()
    {
        return id;
    }

}
