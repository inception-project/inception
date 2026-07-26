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
package de.tudarmstadt.ukp.inception.workload.extension;

/**
 * The canonical curation-readiness rules. These are the single source of truth for the question
 * "would the workload manager consider annotation on this document complete enough to curate it?".
 * <p>
 * The rules live here as static functions over plain counts so that they can be evaluated both from
 * a {@link WorkloadManagerExtension} (which loads the counts from the database for a single
 * document) and from the workload management pages (which already hold bulk-loaded counts for every
 * row and must not issue a query per row).
 * <p>
 * <b>Mind:</b> readiness is an <b>entry condition</b> for curation, not a continuous invariant. The
 * predicates below are non-monotone - e.g. adding an annotator to a project increases the required
 * count for every document at once - so they may be used to inform the user, but they must never
 * drive an automatic transition out of a curation state.
 */
public final class CurationReadiness
{
    private CurationReadiness()
    {
        // No instances
    }

    /**
     * The static-assignment (matrix) rule: every annotator must either have finished the document
     * or have locked it.
     *
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aIgnoreCount
     *            number of annotators that have locked the document.
     * @param aAnnotatorCount
     *            number of annotators in the project.
     * @return whether the document is ready for curation.
     */
    public static boolean isReadyForCurationAllRequired(long aFinishedCount, long aIgnoreCount,
            long aAnnotatorCount)
    {
        return (aFinishedCount + aIgnoreCount) >= aAnnotatorCount;
    }

    /**
     * The dynamic-assignment rule: <i>enough</i> annotators must have finished the document - not
     * all of them.
     *
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aRequiredAnnotatorCount
     *            number of annotators the project requires to finish a document. Mind that the
     *            corresponding trait is called {@code defaultNumberOfAnnotations} - it counts
     *            annotators, not annotations.
     * @return whether the document is ready for curation.
     */
    public static boolean isReadyForCurationEnoughRequired(long aFinishedCount,
            long aRequiredAnnotatorCount)
    {
        return aFinishedCount >= aRequiredAnnotatorCount;
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aIgnoreCount
     *            number of annotators that have locked the document.
     * @param aAnnotatorCount
     *            number of annotators in the project.
     * @return a message describing the shortfall according to
     *         {@link #isReadyForCurationAllRequired}.
     */
    public static CurationReadinessWarning allRequiredWarning(long aFinishedCount,
            long aIgnoreCount, long aAnnotatorCount)
    {
        var missing = aAnnotatorCount - aFinishedCount - aIgnoreCount;
        return new CurationReadinessWarning(missing, aAnnotatorCount, String.format(
                "Annotation of this document is not complete: %d of the %d annotators have neither "
                        + "marked this document as finished nor locked it. Curating now means "
                        + "curating incomplete data.",
                missing, aAnnotatorCount));
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aRequiredAnnotatorCount
     *            number of annotators the project requires to finish a document.
     * @return a message describing the shortfall according to
     *         {@link #isReadyForCurationEnoughRequired}.
     */
    public static CurationReadinessWarning enoughRequiredWarning(long aFinishedCount,
            long aRequiredAnnotatorCount)
    {
        var missing = aRequiredAnnotatorCount - aFinishedCount;
        return new CurationReadinessWarning(missing, aRequiredAnnotatorCount, String.format(
                "Annotation of this document is not complete: %d of the %d required annotators "
                        + "have not marked this document as finished. Curating now means curating "
                        + "incomplete data.",
                missing, aRequiredAnnotatorCount));
    }
}
