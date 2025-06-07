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
package de.tudarmstadt.ukp.inception.remoteapi;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

public class AnnotationDocumentStateUtils
{

    public static final String ANNOTATION_STATE_COMPLETE = "COMPLETE";
    public static final String ANNOTATION_STATE_IN_PROGRESS = "IN-PROGRESS";
    public static final String ANNOTATION_STATE_LOCKED = "LOCKED";
    public static final String ANNOTATION_STATE_NEW = "NEW";

    public static String annotationDocumentStateToString(AnnotationDocumentState aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case NEW:
            return ANNOTATION_STATE_NEW;
        case FINISHED:
            return ANNOTATION_STATE_COMPLETE;
        case IGNORE:
            return ANNOTATION_STATE_LOCKED;
        case IN_PROGRESS:
            return ANNOTATION_STATE_IN_PROGRESS;
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }

    public static AnnotationDocumentState parseAnnotationDocumentState(String aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case ANNOTATION_STATE_NEW:
            return AnnotationDocumentState.NEW;
        case ANNOTATION_STATE_COMPLETE:
            return AnnotationDocumentState.FINISHED;
        case ANNOTATION_STATE_LOCKED:
            return AnnotationDocumentState.IGNORE;
        case ANNOTATION_STATE_IN_PROGRESS:
            return AnnotationDocumentState.IN_PROGRESS;
        default:
            throw new IllegalArgumentException(
                    "Unknown annotation document state [" + aState + "]");
        }
    }

}
