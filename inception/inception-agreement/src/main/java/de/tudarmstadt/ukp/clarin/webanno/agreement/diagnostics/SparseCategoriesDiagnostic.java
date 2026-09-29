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
package de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SPARSE_CATEGORIES;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Labels that occur too rarely to judge how consistently the raters apply them. The labels
 * themselves are the finding, so - unlike other diagnostics - an instance that stands for several
 * comparisons names the labels of all of them rather than those of the first one observed.
 */
public class SparseCategoriesDiagnostic
    extends AgreementDiagnostic
{
    private static final long serialVersionUID = -3071766153290446313L;

    private static final int MAX_LISTED = 3;

    private final ArrayList<String> labels;
    private final int limit;
    private final boolean merged;

    public SparseCategoriesDiagnostic(Collection<String> aLabels, int aLimit)
    {
        this(new ArrayList<>(new TreeSet<>(aLabels)), aLimit, false);
    }

    private SparseCategoriesDiagnostic(ArrayList<String> aSortedLabels, int aLimit, boolean aMerged)
    {
        // Not quantitative: how many labels are sparse in each comparison says little once the
        // labels of all comparisons are named, so there is no value whose range would be shown.
        super(SPARSE_CATEGORIES, describe(aSortedLabels, aLimit, aMerged), Double.NaN, null);
        labels = aSortedLabels;
        limit = aLimit;
        merged = aMerged;
    }

    /**
     * @return the sparse labels, sorted.
     */
    public List<String> getLabels()
    {
        return unmodifiableList(labels);
    }

    @Override
    public AgreementDiagnostic mergeWith(AgreementDiagnostic aOther)
    {
        if (!(aOther instanceof SparseCategoriesDiagnostic other)) {
            return this;
        }

        var union = new TreeSet<>(labels);
        union.addAll(other.labels);
        // Within a run every comparison uses the same settings, so the limits cannot differ.
        return new SparseCategoriesDiagnostic(new ArrayList<>(union), limit, true);
    }

    private static String describe(List<String> aLabels, int aLimit, boolean aMerged)
    {
        // Each label carries its own quotes so that the "and N more" trailer - which is not a
        // label - stays outside of them.
        var quoted = aLabels.stream().map(AgreementDiagnostics::quoteLabel).toList();
        var listed = quoted.size() <= MAX_LISTED ? String.join(", ", quoted)
                : String.join(", ", quoted.subList(0, MAX_LISTED)) + " and "
                        + (quoted.size() - MAX_LISTED) + " more";

        // A merged instance names the labels of several comparisons, and a label that is sparse in
        // one of them need not be in the others.
        return format("%d label(s) are used on fewer than %d items%s (%s).", aLabels.size(), aLimit,
                aMerged ? " in at least one comparison" : "", listed);
    }
}
