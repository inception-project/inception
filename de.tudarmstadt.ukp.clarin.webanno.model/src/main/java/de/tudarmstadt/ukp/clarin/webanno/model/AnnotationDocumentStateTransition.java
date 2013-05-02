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
 * Variables for the different transitions states of a {@link AnnotationDocument} workflow.
 *
 * @author Seid Muhie Yimam
 *
 */
public enum AnnotationDocumentStateTransition
{
    /**
     * Implicit, first time the annotator opened the document for annotation
     */
    NEWTOANNOTATIONINPROGRESS,
    /**
     * Explicit annotator action
     */
    ANNOTATIONINPROGRESSTOANNOTATIONFINISHED,
    /**
     * explicit annotator action, only possible if associated document in states
     * "annotation in progress" or "annotation finished"
     */
    ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS,
    /**
     * Ignore this annotation document from further processing
     */
    NEWTOIGNOR,
    /**
     * Change document state from IGNOR back to NEW
     */
    IGNORTONEW;

    public static AnnotationDocumentState transition(AnnotationDocumentStateTransition aTransition)
        throws IllegalArgumentException
    {
        if (aTransition.equals(NEWTOANNOTATIONINPROGRESS)) {
            return AnnotationDocumentState.INPROGRESS;
        }
        else if (aTransition.equals(ANNOTATIONINPROGRESSTOANNOTATIONFINISHED)) {
            return AnnotationDocumentState.FINISHED;
        }
        else if (aTransition.equals(ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS)) {
            return AnnotationDocumentState.INPROGRESS;
        }
        else if (aTransition.equals(NEWTOIGNOR)) {
            return AnnotationDocumentState.IGNOR;
        }
        else if (aTransition.equals(IGNORTONEW)) {
            return AnnotationDocumentState.NEW;
        }
        else {
            throw new IllegalArgumentException();
        }
    }

}
