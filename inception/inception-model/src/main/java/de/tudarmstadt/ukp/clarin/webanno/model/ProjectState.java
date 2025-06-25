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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.stream.Stream;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;
import de.tudarmstadt.ukp.inception.support.wicket.HasSymbol;

/**
 * Variables for the different states of a {@link Project} workflow.
 */
public enum ProjectState
    implements PersistentEnum, HasSymbol
{
    /**
     * All annotations of all documents are in state new.
     */
    NEW("NEW", "<i class=\"far fa-circle\"></i>", "black", false),

    /**
     * At least one annotation document has been created for the document
     */
    ANNOTATION_IN_PROGRESS("ANNOTATION_INPROGRESS", "<i class=\"far fa-play-circle\"></i>", "black",
            false),

    /**
     * All annotations have marked their annotation document as finished
     */
    ANNOTATION_FINISHED("ANNOTATION_FINISHED", "<i class=\"far fa-check-circle\"></i>", "green",
            true),

    /**
     * Curation on at least one document has started.
     */
    CURATION_IN_PROGRESS("CURATION_INPROGRESS", "<i class=\"fas fa-clipboard\"></i>", "blue", true),

    /**
     * All documents have been curated
     */
    CURATION_FINISHED("CURATION_FINISHED", "<i class=\"fas fa-clipboard-check\"></i>", "red", true);

    private static final List<ProjectState> ANNOTATION_FINAL_STATES;

    static {
        ANNOTATION_FINAL_STATES = Stream.of(values()) //
                .filter(ProjectState::isAnnotationFinal) //
                .collect(toUnmodifiableList());
    }

    private final String id;
    private final String symbol;
    private final String color;

    /**
     * An annotation-final state indicates that no changes to the annotations are allowed anymore.
     */
    private final boolean annotationFinal;

    ProjectState(String aId, String aSymbol, String aColor, boolean aAnnotationFinal)
    {
        id = aId;
        symbol = aSymbol;
        color = aColor;
        annotationFinal = aAnnotationFinal;
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

    public boolean isAnnotationFinal()
    {
        return annotationFinal;
    }

    @Override
    public String toString()
    {
        return getId();
    }

    public static ProjectState defaultState()
    {
        return NEW;
    }

    public static List<ProjectState> annotationFinalStates()
    {
        return ANNOTATION_FINAL_STATES;
    }

    @Override
    public String symbol()
    {
        return symbol;
    }
}
