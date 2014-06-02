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
    NEW_TO_ANNOTATION_IN_PROGRESS,
    /**
     * Implicit based on annotation document states
     */
    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED,
    /**
     * Explicit curator action
     */
    ANNOTATION_FINISHED_TO_CURATION_IN_PROGRESS,
    /**
     * Explicit curator action
     */
    CURATION_IN_PROGRESS_TO_CURATION_FINISHED,
    /**
     * Admin re-open curation document
     */
    CURATION_FINISHED_TO_CURATION_IN_PROGRESS,
    /**
     * Implicit when admin re-open at least one annotation document
     */
    ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;

    public static SourceDocumentState transition(SourceDocumentStateTransition aTransition)
        throws IllegalArgumentException
    {
        if (aTransition.equals(NEW_TO_ANNOTATION_IN_PROGRESS)) {
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        }
        else if (aTransition.equals(ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED)) {
            return SourceDocumentState.ANNOTATION_FINISHED;
        }
        else if (aTransition.equals(ANNOTATION_FINISHED_TO_CURATION_IN_PROGRESS)) {
            return SourceDocumentState.CURATION_IN_PROGRESS;
        }
        else if (aTransition.equals(CURATION_IN_PROGRESS_TO_CURATION_FINISHED)) {
            return SourceDocumentState.CURATION_FINISHED;
        }
        else if (aTransition.equals(ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS)) {
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        }
        else if (aTransition.equals(CURATION_FINISHED_TO_CURATION_IN_PROGRESS)) {
            return SourceDocumentState.CURATION_IN_PROGRESS;
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
