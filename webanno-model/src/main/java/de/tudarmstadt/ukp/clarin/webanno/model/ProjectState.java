/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.model;

import de.tudarmstadt.ukp.clarin.webanno.support.PersistentEnum;

/**
 * Variables for the different states of a {@link Project} workflow.
 */
public enum ProjectState
    implements PersistentEnum
{
    /**
     * All annotations of all documents are in state new.
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
     * All documents have been curated
     */
    CURATION_FINISHED("CURATION_FINISHED", "red"),
    
    /**
     * Curation on at least one document has started.
     */
    CURATION_IN_PROGRESS("CURATION_INPROGRESS", "blue");
    
    private final String id;
    private final String color;

    ProjectState(String aId, String aColor)
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
