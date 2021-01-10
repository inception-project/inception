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
 * Variables for the different transitions states of a {@link AnnotationDocument} workflow.
 *
 *
 */
public enum AnnotationDocumentStateTransition
{
    /**
     * Implicit, first time the annotator opened the document for annotation
     */
    NEW_TO_ANNOTATION_IN_PROGRESS,
    /**
     * Explicit annotator action
     */
    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED,
    /**
     * explicit annotator action, only possible if associated document in states "annotation in
     * progress" or "annotation finished"
     */
    ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS,
    /**
     * Ignore this annotation document from further processing
     */
    NEW_TO_IGNORE,
    /**
     * Change document state from IGNORE back to NEW
     */
    IGNORE_TO_NEW;

    public static AnnotationDocumentState transition(AnnotationDocumentStateTransition aTransition)
        throws IllegalArgumentException
    {
        if (aTransition.equals(NEW_TO_ANNOTATION_IN_PROGRESS)) {
            return AnnotationDocumentState.IN_PROGRESS;
        }
        else if (aTransition.equals(ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED)) {
            return AnnotationDocumentState.FINISHED;
        }
        else if (aTransition.equals(ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS)) {
            return AnnotationDocumentState.IN_PROGRESS;
        }
        else if (aTransition.equals(NEW_TO_IGNORE)) {
            return AnnotationDocumentState.IGNORE;
        }
        else if (aTransition.equals(IGNORE_TO_NEW)) {
            return AnnotationDocumentState.NEW;
        }
        else {
            throw new IllegalArgumentException();
        }
    }

}
