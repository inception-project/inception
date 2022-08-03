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

/**
 * Variables for the different transitions states of a {@link SourceDocument} workflow.
 */
public enum SourceDocumentStateTransition
{
    /**
     * Implicit based on annotation document states (new when the document uploaded to the project)
     */
    NEW_TO_ANNOTATION_IN_PROGRESS,

    /**
     * Implicit based on annotation document states
     * 
     * @deprecated This is not used and should not be used. Will be removed in future versions. If
     *             you want to tell whether all annotators have marked a document as finished, you
     *             have to manually check if all annotators assigned to annotate this document have
     *             marked their annotation documents as done. This is nothing we can record
     *             statically in the source document.
     */
    @Deprecated
    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED,

    /**
     * Explicit curator action
     */
    ANNOTATION_FINISHED_TO_CURATION_IN_PROGRESS,

    /**
     * Explicit curator action - can be used to transition a document into curation state even if it
     * has never been opened in the curation editor.
     */
    ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS,

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
     * 
     * @deprecated This is not used and should not be used. Will be removed in future versions. If
     *             you want to tell whether all annotators have marked a document as finished, you
     *             have to manually check if all annotators assigned to annotate this document have
     *             marked their annotation documents as done. This is nothing we can record
     *             statically in the source document.
     */
    @Deprecated
    ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;

    public static SourceDocumentState transition(SourceDocumentStateTransition aTransition)
    {
        if (aTransition.equals(NEW_TO_ANNOTATION_IN_PROGRESS)) {
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        }
        else if (aTransition.equals(ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED)) {
            return SourceDocumentState.ANNOTATION_FINISHED;
        }
        else if (aTransition.equals(ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS)) {
            return SourceDocumentState.CURATION_IN_PROGRESS;
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
            throw new IllegalArgumentException(
                    "Cannot apply source document transition [" + aTransition + "]");
        }
    }
}
