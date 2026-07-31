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
package de.tudarmstadt.ukp.inception.workload.matrix;

import static java.lang.Math.max;
import static java.lang.String.format;

import de.tudarmstadt.ukp.inception.workload.extension.CurationReadinessWarning;

/**
 * The curation-readiness rules of the static-assignment (matrix) regime: <b>every</b> annotator
 * must either have finished the document or have locked it, and at least one must actually have
 * finished it.
 * <p>
 * These are static functions over plain counts so that the matrix management page can evaluate them
 * against the counts it already holds per row, without a query per row.
 * <p>
 * Readiness is an entry condition, not a continuous invariant.
 */
public final class MatrixCurationReadiness
{
    private MatrixCurationReadiness()
    {
        // No instances
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aIgnoreCount
     *            number of annotators that have locked the document.
     * @param aAnnotatorCount
     *            number of annotators in the project.
     * @return whether the document is ready for curation.
     */
    public static boolean isReadyForCuration(long aFinishedCount, long aIgnoreCount,
            long aAnnotatorCount)
    {
        return aFinishedCount > 0 && (aFinishedCount + aIgnoreCount) >= aAnnotatorCount;
    }

    /**
     * @param aFinishedCount
     *            number of annotators that have finished the document.
     * @param aIgnoreCount
     *            number of annotators that have locked the document.
     * @param aAnnotatorCount
     *            number of annotators in the project.
     * @return a message describing the shortfall according to {@link #isReadyForCuration}.
     */
    public static CurationReadinessWarning warning(long aFinishedCount, long aIgnoreCount,
            long aAnnotatorCount)
    {
        if (!(aFinishedCount > 0)) {
            return nothingFinishedWarning(aAnnotatorCount);
        }

        var missing = max(0, aAnnotatorCount - aFinishedCount - aIgnoreCount);
        return new CurationReadinessWarning(missing, aAnnotatorCount, format(
                "Annotation of this document is not complete: %d of the %d annotators have neither "
                        + "marked this document as finished nor locked it. Curating now means "
                        + "curating incomplete data.",
                missing, aAnnotatorCount));
    }

    /**
     * Handled separately from the shortfall arithmetic above because the counts may well add up to
     * the threshold - e.g. every annotator locked the document - while there is still nothing to
     * curate. Since nobody finished it, every annotator of the project is outstanding.
     * <p>
     * The message must not claim that there is no annotation data at all: the finished count is
     * scoped to the users currently holding the annotator role, so a document that only former
     * annotators finished counts as zero here while curation opens it with their data.
     *
     * @param aAnnotatorCount
     *            number of annotators in the project.
     * @return a message for the case that no annotator finished the document at all.
     */
    private static CurationReadinessWarning nothingFinishedWarning(long aAnnotatorCount)
    {
        var message = aAnnotatorCount == 0
                ? "This project has no annotators, so nobody can have marked this document as "
                        + "finished. Curating now means curating incomplete data - if any annotation data "
                        + "exists at all."
                : format("None of the %d annotators of this project has marked this document as "
                        + "finished. Curating now means curating incomplete data - if any "
                        + "annotation data exists at all.", aAnnotatorCount);

        return new CurationReadinessWarning(aAnnotatorCount, aAnnotatorCount, message);
    }
}
