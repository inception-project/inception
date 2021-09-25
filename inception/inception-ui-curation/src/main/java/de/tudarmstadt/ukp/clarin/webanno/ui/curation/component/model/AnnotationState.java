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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

/**
 * State of an annotated span (or arc). Contains color code for visualization
 */
public enum AnnotationState
{
    /**
     * Curator selected this configuration.
     */
    ACCEPTED_BY_CURATOR("#77dd77"), // cf. "curated" class in CurationPage.html
    /**
     * Curator did choose a configuration but not this one. The configuration the curator chose does
     * not have to match any configuration that provided by the annotators - it can be a completely
     * new one.
     */
    REJECTED_BY_CURATOR("#0dcaf0"), // Bootstrap 5 cyan
    /**
     * All annotators agree but the curator did not make a choice yet.
     */
    // Bootstrap 5 orange - cannot use "agree" class in CurationPage.html because that is white and
    // that would cause relations to become invisible.

    ANNOTATORS_AGREE("#fd7e14"),
    /**
     * Annotators disagree and curator did not make a decision yet.
     */
    ANNOTATORS_DISAGREE("#FF9999"), // cf. "disagree" class in CurationPage.html
    /**
     * Annotators have not all annotated (incomplete) and curator did not make a decision yet.
     */
    ANNOTATORS_INCOMPLETE("#DDA0DD"), // cf. "incomplete" class in CurationPage.html
    /**
     * Error state. Annotation has been added to the visualization, but has not been identified by
     * the CasDiff.
     */
    ERROR("#111111"); // almost black

    private String colorCode;

    AnnotationState(String aColorCode)
    {
        colorCode = aColorCode;
    }

    public String getColorCode()
    {
        return colorCode;
    }
}
