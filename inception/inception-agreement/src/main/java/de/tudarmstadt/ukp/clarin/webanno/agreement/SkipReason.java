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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

/**
 * Why a comparison produced no result for a document.
 */
public enum SkipReason
{
    /**
     * The comparison involves the curator, but the document is not in a curation state.
     */
    NOT_IN_CURATION_STATE,

    /**
     * One of the raters has no annotation document in the states included in the calculation - e.g.
     * because they have not started it, or have not finished it while the calculation is limited to
     * finished documents. The rater is recorded along with the reason.
     */
    NOT_ANNOTATED,

    /**
     * Loading the data or calculating the agreement failed. The details are in the log. If the data
     * of a rater could not be loaded, that rater is recorded along with the reason.
     */
    FAILED;
}
