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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class AgreementConcern
    implements Serializable
{
    private static final long serialVersionUID = -1097818552873509368L;

    private final AgreementDiagnostic diagnostic;
    private final String implication;
    private final String suggestion;

    public AgreementConcern(AgreementDiagnostic aDiagnostic, String aImplication,
            String aSuggestion)
    {
        diagnostic = aDiagnostic;
        implication = aImplication;
        suggestion = aSuggestion;
    }

    public AgreementConcern(AgreementDiagnostic aDiagnostic, String aImplication)
    {
        this(aDiagnostic, aImplication, null);
    }

    public AgreementDiagnostic getDiagnostic()
    {
        return diagnostic;
    }

    public AgreementDiagnosticType getType()
    {
        return diagnostic.getType();
    }

    /**
     * @return what was observed in the data.
     */
    public String getObservation()
    {
        return diagnostic.getObservation();
    }

    /**
     * @return what the observation means for the score that was computed.
     */
    public String getImplication()
    {
        return implication;
    }

    /**
     * @return what the user could do about it, if there is a concrete option.
     */
    public Optional<String> getSuggestion()
    {
        return Optional.ofNullable(suggestion);
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }

        if (!(aOther instanceof AgreementConcern other)) {
            return false;
        }

        return Objects.equals(diagnostic, other.diagnostic);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(diagnostic);
    }

    @Override
    public String toString()
    {
        return "AgreementConcern [" + getType() + ": " + implication + "]";
    }
}
