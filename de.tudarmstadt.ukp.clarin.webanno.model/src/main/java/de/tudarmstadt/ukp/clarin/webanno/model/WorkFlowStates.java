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
 * Variables for the different states of a workflow.
 *
 * @author Seid Muhie Yimam
 *
 */
public class WorkFlowStates
{
    /**
     * no annotation document has been created for this document
     * OR
     * State: new - no annotation document has been created for this document for this annotator
     */
    public static final String NEW = "new";
    /**
     * at least one annotation document has been created for the document
     * OR
     * annotation document has been created for this document for this annotator
     */
    public static final String ANNOTATION_IN_PROGRESS = "annotation in progress";
    /**
     * all annotations have marked their annotation document as finished
     * OR
     *  annotator has marked annotation document as complete
     */
    public static final String ANNOTATION_FINISHED = "annotation finished";
    /**
     * curator has started working with the annotation document, annotators can no longer make
     * modifications in annotation documents
     */
    public static final String CURATION_IN_PROGRESS = "curation in progress";
    /**
     * curator claims to have curated all annotations
     */
    public static final String CURATION_FINISHED = "curation finished";

}
