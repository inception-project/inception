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
package de.tudarmstadt.ukp.inception.workload.dynamic;

import de.tudarmstadt.ukp.inception.workload.extension.CurationReadinessWarning;

/**
 * The curation-readiness rules of the dynamic-assignment regime: <i>enough</i> annotators must have
 * finished the document - not all of them - and at least one must actually have finished it.
 * <p>
 * Unlike the static-assignment rule, the threshold is a project setting rather than a headcount, so
 * who currently holds the annotator role does not enter the arithmetic. The finished count covers
 * all data owners, including former annotators.
 * <p>
 * These are static functions over plain counts so that the dynamic management page can evaluate
 * them against the counts it already holds per row, without a query per row.
 * <p>
 * Readiness is an entry condition, not a continuous invariant.
 */
public final class DynamicCurationReadiness
{
    private DynamicCurationReadiness()
    {
        // No instances
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aRequiredAnnotatorCount
     *            number of annotators the project requires to finish a document. Mind that the
     *            corresponding trait is called {@code defaultNumberOfAnnotations} - it counts
     *            annotators, not annotations.
     * @return whether the document is ready for curation.
     */
    public static boolean isReadyForCuration(long aFinishedCount, long aRequiredAnnotatorCount)
    {
        return aFinishedCount > 0 && aFinishedCount >= aRequiredAnnotatorCount;
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aRequiredAnnotatorCount
     *            number of annotators the project requires to finish a document.
     * @return a message describing the shortfall according to {@link #isReadyForCuration}.
     */
    public static CurationReadinessWarning warning(long aFinishedCount,
            long aRequiredAnnotatorCount)
    {
        if (!(aFinishedCount > 0)) {
            return nothingFinishedWarning(aRequiredAnnotatorCount);
        }

        // Reached only when 0 < aFinishedCount < aRequiredAnnotatorCount, so no clamp is needed.
        var missing = aRequiredAnnotatorCount - aFinishedCount;
        return new CurationReadinessWarning(missing, aRequiredAnnotatorCount, String.format(
                "Annotation of this document is not complete: %d of the %d required annotators "
                        + "have not marked this document as finished. Curating now means curating "
                        + "incomplete data.",
                missing, aRequiredAnnotatorCount));
    }

    /**
     * Handled separately from the shortfall arithmetic above because a required count of zero
     * satisfies the threshold while there is still nothing to curate. Since nobody finished it, the
     * full required count is outstanding, so a project requiring nobody reports {@code 0/0} and the
     * explanation is left to the message.
     *
     * @param aRequiredAnnotatorCount
     *            number of annotators the project requires to finish a document.
     * @return a message for the case that no annotator finished the document at all.
     */
    private static CurationReadinessWarning nothingFinishedWarning(long aRequiredAnnotatorCount)
    {
        var message = aRequiredAnnotatorCount == 0
                ? "This project requires no annotator to finish a document, and none has. Set the "
                        + "number of required annotations for this project to define when a "
                        + "document counts as fully annotated. Curating now means curating "
                        + "incomplete data - if any annotation data exists at all."
                : String.format(
                        "No annotator has marked this document as finished yet - %d are required. "
                                + "Curating now means curating incomplete data - if any annotation "
                                + "data exists at all.",
                        aRequiredAnnotatorCount);

        return new CurationReadinessWarning(aRequiredAnnotatorCount, aRequiredAnnotatorCount,
                message);
    }
}
