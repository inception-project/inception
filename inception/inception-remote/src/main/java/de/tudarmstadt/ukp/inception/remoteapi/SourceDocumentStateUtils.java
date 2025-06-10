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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class SourceDocumentStateUtils
{
    public static final String DOCUMENT_STATE_NEW = "NEW";
    public static final String DOCUMENT_STATE_ANNOTATION_IN_PROGRESS = "ANNOTATION-IN-PROGRESS";
    public static final String DOCUMENT_STATE_ANNOTATION_COMPLETE = "ANNOTATION-COMPLETE";
    public static final String DOCUMENT_STATE_CURATION_COMPLETE = "CURATION-COMPLETE";
    public static final String DOCUMENT_STATE_CURATION_IN_PROGRESS = "CURATION-IN-PROGRESS";

    public static SourceDocumentState parseSourceDocumentState(String aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case DOCUMENT_STATE_NEW:
            return SourceDocumentState.NEW;
        case DOCUMENT_STATE_ANNOTATION_IN_PROGRESS:
            return SourceDocumentState.ANNOTATION_IN_PROGRESS;
        case DOCUMENT_STATE_ANNOTATION_COMPLETE:
            return SourceDocumentState.ANNOTATION_FINISHED;
        case DOCUMENT_STATE_CURATION_IN_PROGRESS:
            return SourceDocumentState.CURATION_IN_PROGRESS;
        case DOCUMENT_STATE_CURATION_COMPLETE:
            return SourceDocumentState.CURATION_FINISHED;
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

    public static String sourceDocumentStateToString(SourceDocumentState aState)
    {
        if (aState == null) {
            return null;
        }

        switch (aState) {
        case NEW:
            return DOCUMENT_STATE_NEW;
        case ANNOTATION_IN_PROGRESS:
            return DOCUMENT_STATE_ANNOTATION_IN_PROGRESS;
        case ANNOTATION_FINISHED:
            return DOCUMENT_STATE_ANNOTATION_COMPLETE;
        case CURATION_IN_PROGRESS:
            return DOCUMENT_STATE_CURATION_IN_PROGRESS;
        case CURATION_FINISHED:
            return DOCUMENT_STATE_CURATION_COMPLETE;
        default:
            throw new IllegalArgumentException("Unknown source document state [" + aState + "]");
        }
    }

}
