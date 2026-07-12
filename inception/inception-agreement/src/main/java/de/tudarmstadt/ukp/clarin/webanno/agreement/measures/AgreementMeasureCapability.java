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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

/**
 * Non-exclusive capabilities of an agreement measure, rendered as badges/filters in the selection
 * UI. These mirror the capability marker interfaces in DKPro Statistics agreement, but are declared
 * statically per measure support (rather than detected from a constructed measure instance) so they
 * are available before a measure has been configured.
 */
public enum AgreementMeasureCapability
{
    /**
     * The score is corrected for the agreement expected by chance
     * ({@code IChanceCorrectedAgreement} / {@code IChanceCorrectedDisagreement}). Chance-corrected
     * measures are comparable across studies with different label distributions.
     */
    CHANCE_CORRECTED("Chance-corrected", //
            "Corrects for agreement that would be expected by chance, making the score comparable "
                    + "across studies."),

    /**
     * Supports more than two raters in a single computation ({@code IMultiRaterAgreement}).
     * Measures without this capability are pairwise-only.
     */
    MULTI_RATER("Multi-rater", //
            "Can be computed over more than two annotators at once, rather than pairwise only."),

    /**
     * Uses a distance/weighting function so that near-misses count as partial agreement
     * ({@code IWeightedAgreement}).
     */
    WEIGHTED("Weighted", //
            "Uses a distance function so that near-misses count as partial rather than total "
                    + "disagreement."),

    /**
     * Can report agreement broken down per category/label ({@code ICategorySpecificAgreement}).
     */
    CATEGORY_SPECIFIC("Per-category", //
            "Can report agreement broken down per label, not just an overall score.");

    private final String uiName;
    private final String description;

    AgreementMeasureCapability(String aUiName, String aDescription)
    {
        uiName = aUiName;
        description = aDescription;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getDescription()
    {
        return description;
    }
}
