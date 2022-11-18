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

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.support.PersistentEnum;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.HasSymbol;

/**
 * Variables for the different states of a {@link SourceDocument} workflow.
 */
public enum SourceDocumentState
    implements PersistentEnum, HasSymbol
{
    /**
     * No annotation document has been created for this document
     */
    @JsonProperty("NEW")
    NEW("NEW", "<i class=\"far fa-circle\"></i>", "black"),

    /**
     * At least one annotation document has been created for the document
     */
    @JsonProperty("ANNOTATION_IN_PROGRESS")
    ANNOTATION_IN_PROGRESS("ANNOTATION_INPROGRESS", "<i class=\"far fa-play-circle\"></i>",
            "black"),

    /**
     * All annotations have marked their annotation document as finished
     */
    @JsonProperty("ANNOTATION_FINISHED")
    ANNOTATION_FINISHED("ANNOTATION_FINISHED", "<i class=\"far fa-check-circle\"></i>", "green"),

    /**
     * curator has started working with the annotation document, annotators can no longer make
     * modifications in annotation documents
     */
    @JsonProperty("CURATION_IN_PROGRESS")
    CURATION_IN_PROGRESS("CURATION_INPROGRESS", "<i class=\"fas fa-clipboard\"></i>", "blue"),

    /**
     * curator claims to have curated all annotations
     */
    @JsonProperty("CURATION_FINISHED")
    CURATION_FINISHED("CURATION_FINISHED", "<i class=\"fas fa-clipboard-check\"></i>", "red");

    private final String id;
    private final String symbol;
    private final String color;

    SourceDocumentState(String aId, String aSymbol, String aColor)
    {
        id = aId;
        symbol = aSymbol;
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
    public String symbol()
    {
        return symbol;
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
