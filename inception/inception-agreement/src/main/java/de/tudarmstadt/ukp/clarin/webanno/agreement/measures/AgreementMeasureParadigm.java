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
 * The paradigm an agreement measure operates in. This corresponds to the top-level interface
 * families in DKPro Statistics agreement ({@code ICodingAgreementMeasure},
 * {@code IUnitizingAgreementMeasure}, {@code IAligningAgreementMeasure}) and determines what kind
 * of annotation study the measure consumes. Every measure belongs to exactly one paradigm, which
 * makes it the primary axis for grouping measures in the selection UI.
 */
public enum AgreementMeasureParadigm
{
    /**
     * Compares annotations that have been aligned into matching units and scores agreement on their
     * categories/labels. Requires a categorical feature.
     */
    CODING("Coding", //
            "Compares the labels assigned to matching annotations. Suitable when annotators work "
                    + "from a fixed set of positions and you want to know how often they chose the "
                    + "same category."),

    /**
     * Scores agreement on how annotators segment the text into units, based on character offsets. A
     * feature is optional: without one only the segmentation is compared, with one the labels of
     * the units count as well.
     */
    UNITIZING("Unitizing", //
            "Compares how annotators segment the text into units based on character offsets. "
                    + "Suitable when the disagreement of interest is about boundaries - the "
                    + "labels of the units are taken into account only if a feature is selected."),

    /**
     * Aligns annotations across raters as part of computing agreement, tolerating differences in
     * both boundaries and categories.
     */
    ALIGNING("Aligning", //
            "Aligns annotations across raters while scoring agreement, tolerating differences in "
                    + "both boundaries and labels.");

    private final String uiName;
    private final String description;

    AgreementMeasureParadigm(String aUiName, String aDescription)
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
