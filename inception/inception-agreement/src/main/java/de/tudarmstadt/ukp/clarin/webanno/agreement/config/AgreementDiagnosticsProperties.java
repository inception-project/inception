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
package de.tudarmstadt.ukp.clarin.webanno.agreement.config;

/**
 * Thresholds at which the agreement diagnostics report a characteristic of the data.
 * <p>
 * These are conventions rather than derived constants - where the line falls between "worth
 * mentioning" and "noise" depends on the annotation project. The defaults are chosen to stay quiet
 * on well-behaved data, since an advisory that appears on every result gets ignored; projects that
 * want to be told earlier (or later) can move them.
 * <p>
 * Two kinds of threshold appear here, and the kind follows from what the diagnostic claims:
 * <ul>
 * <li>Claims about <em>how much evidence</em> there is are absolute counts. Reliability does not
 * scale with study size: four observations of a label are too few to interpret whether the study
 * has 50 items or 50,000.</li>
 * <li>Claims about <em>the shape</em> of the data are shares. "One label dominates" has no absolute
 * reading.</li>
 * </ul>
 * Share-based checks additionally observe {@link #getMinimumBasisForShare()} as a noise floor: a
 * percentage computed over a handful of items is arithmetic rather than evidence. That floor is
 * only a guard - it never appears in the text shown to the user.
 */
public interface AgreementDiagnosticsProperties
{
    double getSkewThreshold();

    double getMarginalAsymmetryThreshold();

    int getSparseCategoryLimit();

    int getFewItemsLimit();

    double getBoundaryDisagreementThreshold();

    double getUnusableDocumentThreshold();

    double getAffectedDocumentThreshold();

    int getMinimumBasisForShare();
}
