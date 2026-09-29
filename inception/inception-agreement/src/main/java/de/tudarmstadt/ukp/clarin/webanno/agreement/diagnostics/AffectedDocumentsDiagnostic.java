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

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.TreeSet;

public class AffectedDocumentsDiagnostic
    extends AgreementDiagnostic
{
    private static final long serialVersionUID = 5520981693144850264L;

    private final String characteristic;
    private final List<String> documentNames;

    /**
     * {@code null} for an instance that stands for several comparisons: their counts are based on
     * different documents and cannot be combined.
     */
    private final Integer affectedCount;

    /**
     * {@code null} for an instance that stands for several comparisons - see
     * {@link #affectedCount}.
     */
    private final Integer scoredCount;

    /**
     * @param aType
     *            the characteristic.
     * @param aCharacteristic
     *            what the documents exhibit, phrased to follow "In 3 of the 10 scored documents",
     *            e.g. "all annotations use the same label".
     * @param aDocumentNames
     *            the names of the affected documents. Documents without a known name are counted,
     *            but not named.
     * @param aAffectedCount
     *            the number of affected documents.
     * @param aScoredCount
     *            the number of documents that produced a score.
     */
    public AffectedDocumentsDiagnostic(AgreementDiagnosticType aType, String aCharacteristic,
            Collection<String> aDocumentNames, int aAffectedCount, int aScoredCount)
    {
        // Boxed explicitly, so this resolves to the private constructor rather than to itself
        this(aType, aCharacteristic, new ArrayList<>(new TreeSet<>(aDocumentNames)),
                Integer.valueOf(aAffectedCount), Integer.valueOf(aScoredCount));
    }

    private AffectedDocumentsDiagnostic(AgreementDiagnosticType aType, String aCharacteristic,
            List<String> aSortedNames, Integer aAffectedCount, Integer aScoredCount)
    {
        // Not quantitative: the documents are the finding, and their share differs between the
        // comparisons whose documents a merged instance names.
        super(aType, describe(aCharacteristic, aSortedNames, aAffectedCount, aScoredCount),
                Double.NaN, null);
        characteristic = aCharacteristic;
        documentNames = aSortedNames;
        affectedCount = aAffectedCount;
        scoredCount = aScoredCount;
    }

    /**
     * Creates an instance that stands for several comparisons. It names the affected documents of
     * all of them, but carries no counts.
     */
    private static AffectedDocumentsDiagnostic merged(AgreementDiagnosticType aType,
            String aCharacteristic, Collection<String> aDocumentNames)
    {
        return new AffectedDocumentsDiagnostic(aType, aCharacteristic,
                new ArrayList<>(new TreeSet<>(aDocumentNames)), null, null);
    }

    /**
     * @return the names of the affected documents, sorted.
     */
    public List<String> getDocumentNames()
    {
        return unmodifiableList(documentNames);
    }

    /**
     * @return the number of affected documents, or nothing for an instance that stands for several
     *         comparisons.
     */
    public OptionalInt getAffectedCount()
    {
        return affectedCount != null ? OptionalInt.of(affectedCount) : OptionalInt.empty();
    }

    /**
     * @return the number of documents that produced a score, or nothing for an instance that stands
     *         for several comparisons.
     */
    public OptionalInt getScoredCount()
    {
        return scoredCount != null ? OptionalInt.of(scoredCount) : OptionalInt.empty();
    }

    @Override
    public AgreementDiagnostic mergeWith(AgreementDiagnostic aOther)
    {
        if (!(aOther instanceof AffectedDocumentsDiagnostic other)) {
            return this;
        }

        var union = new TreeSet<>(documentNames);
        union.addAll(other.documentNames);
        return merged(getType(), characteristic, union);
    }

    private static String describe(String aCharacteristic, List<String> aNames, Integer aAffected,
            Integer aScored)
    {
        if (aScored == null) {
            var listed = AgreementDiagnostics.listDocuments(aNames, aNames.size());
            return format("In at least one comparison, %s in some of the scored documents%s.",
                    aCharacteristic, listed.isEmpty() ? "" : ": " + listed);
        }

        var listed = AgreementDiagnostics.listDocuments(aNames, aAffected);
        return format("In %d of the %d scored documents, %s%s.", aAffected, aScored,
                aCharacteristic, listed.isEmpty() ? "" : ": " + listed);
    }
}
