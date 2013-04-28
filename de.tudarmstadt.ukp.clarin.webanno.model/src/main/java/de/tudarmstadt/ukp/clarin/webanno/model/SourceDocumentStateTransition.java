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
 * Variables for the different transitions states of a {@link SourceDocument} workflow.
 *
 * @author Seid Muhie Yimam
 *
 */
public enum SourceDocumentStateTransition
{
    /**
     * Implicit based on annotation document states (new when the document uploaded to the project)
     */
    NEWTOANNOTATIONINPROGRESS,
    /**
     * Implicit based on annotation document states
     */
    ANNOTATIONINPROGRESSTOANNOTATIONFINISHED,
    /**
     * Explicit curator action
     */
    ANNOTATIONFINISHEDTOCURATIONINPROGRESS,
    /**
     * Explicit curator action
     */
    CURATIONINPROGRESSTOCURATIONFINISHED,
    /**
     * Implicit when admin re-open at least one annotation document
     */
    ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS;

    public static SourceDocumentState transition(SourceDocumentStateTransition aTransition)
        throws IllegalArgumentException
    {
        if (aTransition.equals(NEWTOANNOTATIONINPROGRESS)) {
            return SourceDocumentState.ANNOTATION_INPROGRESS;
        }
        else if (aTransition.equals(ANNOTATIONINPROGRESSTOANNOTATIONFINISHED)) {
            return SourceDocumentState.ANNOTATION_FINISHED;
        }
        else if (aTransition.equals(ANNOTATIONFINISHEDTOCURATIONINPROGRESS)) {
            return SourceDocumentState.CURATION_INPROGRESS;
        }
        else if (aTransition.equals(CURATIONINPROGRESSTOCURATIONFINISHED)) {
            return SourceDocumentState.CURATION_FINISHED;
        }
        else if (aTransition.equals(ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS)) {
            return SourceDocumentState.ANNOTATION_INPROGRESS;
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
