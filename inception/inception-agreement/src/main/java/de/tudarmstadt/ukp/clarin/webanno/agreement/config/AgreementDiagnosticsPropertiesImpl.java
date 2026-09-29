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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link AgreementAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("agreement.diagnostics")
public class AgreementDiagnosticsPropertiesImpl
    implements AgreementDiagnosticsProperties
{
    /**
     * Share of the assigned labels the most frequent label must reach (or exceed) for the
     * distribution to count as skewed. Above this, chance-corrected measures of the Kappa family
     * become unstable (the "Kappa paradox"). Only a single dominant label produces this effect -
     * mass spread across several labels does not - so this deliberately looks at the most frequent
     * label alone.
     */
    private double skewThreshold = 0.85;

    /**
     * Difference in label usage between two raters, as a share of their annotations, at or above
     * which their marginals count as asymmetric.
     */
    private double marginalAsymmetryThreshold = 0.20;

    /**
     * Number of items below which a label is considered too sparse for the agreement on it to be
     * interpretable. Items rather than assignments are counted, so that the same evidence does not
     * count once per rater.
     */
    private int sparseCategoryLimit = 5;

    /**
     * Number of items below which any agreement score is statistically fragile.
     */
    private int fewItemsLimit = 30;

    /**
     * Share of relevant positions missing from at least one rater at or above which a substantial
     * part of the disagreement is about where to annotate rather than which label to use. Coding
     * measures only compare labels at matching positions, so even a fifth of the positions going
     * unmatched is disagreement that a high score does not show.
     */
    private double boundaryDisagreementThreshold = 0.20;

    /**
     * Share of the documents both raters of a pair worked on that must fail to produce a score
     * (reached or exceeded) before that is reported.
     */
    private double unusableDocumentThreshold = 0.30;

    /**
     * Share of the scored documents that must exhibit a characteristic of the label distribution on
     * their own before they are reported, where the data pooled across all documents does not. The
     * score is the average of the per-document scores, so such documents distort it even where the
     * characteristic cancels out across documents.
     */
    private double affectedDocumentThreshold = 0.25;

    /**
     * Smallest number of observations a share may be computed over before it is reported. Guards
     * the share-based diagnostics over labels and positions against firing on a handful of items -
     * "2 of 3 positions" is a true but useless 67% - and is never shown to the user. It does not
     * apply to the share of documents that produced no score, which is not an estimate but an exact
     * account of the selection.
     */
    private int minimumBasisForShare = 20;

    @Override
    public double getSkewThreshold()
    {
        return skewThreshold;
    }

    public void setSkewThreshold(double aSkewThreshold)
    {
        skewThreshold = requireShare("skew-threshold", aSkewThreshold);
    }

    @Override
    public double getMarginalAsymmetryThreshold()
    {
        return marginalAsymmetryThreshold;
    }

    public void setMarginalAsymmetryThreshold(double aMarginalAsymmetryThreshold)
    {
        marginalAsymmetryThreshold = requireShare("marginal-asymmetry-threshold",
                aMarginalAsymmetryThreshold);
    }

    @Override
    public int getSparseCategoryLimit()
    {
        return sparseCategoryLimit;
    }

    public void setSparseCategoryLimit(int aSparseCategoryLimit)
    {
        sparseCategoryLimit = requireAtLeast("sparse-category-limit", aSparseCategoryLimit, 0);
    }

    @Override
    public int getFewItemsLimit()
    {
        return fewItemsLimit;
    }

    public void setFewItemsLimit(int aFewItemsLimit)
    {
        fewItemsLimit = requireAtLeast("few-items-limit", aFewItemsLimit, 0);
    }

    @Override
    public double getBoundaryDisagreementThreshold()
    {
        return boundaryDisagreementThreshold;
    }

    public void setBoundaryDisagreementThreshold(double aBoundaryDisagreementThreshold)
    {
        boundaryDisagreementThreshold = requireShare("boundary-disagreement-threshold",
                aBoundaryDisagreementThreshold);
    }

    @Override
    public double getUnusableDocumentThreshold()
    {
        return unusableDocumentThreshold;
    }

    public void setUnusableDocumentThreshold(double aUnusableDocumentThreshold)
    {
        unusableDocumentThreshold = requireShare("unusable-document-threshold",
                aUnusableDocumentThreshold);
    }

    @Override
    public double getAffectedDocumentThreshold()
    {
        return affectedDocumentThreshold;
    }

    public void setAffectedDocumentThreshold(double aAffectedDocumentThreshold)
    {
        affectedDocumentThreshold = requireShare("affected-document-threshold",
                aAffectedDocumentThreshold);
    }

    @Override
    public int getMinimumBasisForShare()
    {
        return minimumBasisForShare;
    }

    public void setMinimumBasisForShare(int aMinimumBasisForShare)
    {
        minimumBasisForShare = requireAtLeast("minimum-basis-for-share", aMinimumBasisForShare, 1);
    }

    private static double requireShare(String aProperty, double aValue)
    {
        // A negative threshold would flag every study, one above 1 would silently disable the
        // check - neither is what someone setting a share means, so reject both at startup.
        if (Double.isNaN(aValue) || aValue < 0.0 || aValue > 1.0) {
            throw new IllegalArgumentException("agreement.diagnostics." + aProperty
                    + " must be a share between 0 and 1, but was " + aValue);
        }
        return aValue;
    }

    private static int requireAtLeast(String aProperty, int aValue, int aMinimum)
    {
        if (aValue < aMinimum) {
            throw new IllegalArgumentException("agreement.diagnostics." + aProperty
                    + " must be at least " + aMinimum + ", but was " + aValue);
        }
        return aValue;
    }
}
