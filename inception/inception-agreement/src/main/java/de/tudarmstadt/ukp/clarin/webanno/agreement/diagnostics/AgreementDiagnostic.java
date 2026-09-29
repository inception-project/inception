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

/**
 * A single observation about the annotated data, produced by {@link AgreementDiagnostics}. It
 * carries the measured evidence (e.g. the share of the dominant label) so that the UI can state
 * what was actually observed rather than a vague warning.
 */
public class AgreementDiagnostic
    implements Serializable
{
    private static final long serialVersionUID = 7799361409216122069L;

    private final AgreementDiagnosticType type;
    private final String observation;
    private final double value;
    private final String subject;

    public AgreementDiagnostic(AgreementDiagnosticType aType, String aObservation, double aValue,
            String aSubject)
    {
        type = aType;
        observation = aObservation;
        value = aValue;
        subject = aSubject;
    }

    public AgreementDiagnostic(AgreementDiagnosticType aType, String aObservation)
    {
        this(aType, aObservation, Double.NaN, null);
    }

    public AgreementDiagnosticType getType()
    {
        return type;
    }

    /**
     * @return a self-contained statement of what was observed in the data, e.g. "One label
     *         ("OTHER") covers 91% of the annotated items."
     */
    public String getObservation()
    {
        return observation;
    }

    /**
     * @return the measured quantity behind the observation (share, ratio, count), or
     *         {@link Double#NaN} if the diagnostic is not quantitative.
     */
    public double getValue()
    {
        return value;
    }

    /**
     * @return the category or rater the observation is about, if it is about a specific one.
     */
    public Optional<String> getSubject()
    {
        return Optional.ofNullable(subject);
    }

    /**
     * Combines this diagnostic with an equal one observed in another comparison into the instance
     * that stands for both. By default the one observed first stands for all of them, and callers
     * qualify its value using {@code AgreementResult_ImplBase#getDiagnosticValueRange}. Subclasses
     * override this where the observation itself has to cover every comparison.
     *
     * @param aOther
     *            an equal diagnostic from another comparison.
     * @return the diagnostic standing for both.
     */
    public AgreementDiagnostic mergeWith(AgreementDiagnostic aOther)
    {
        return this;
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }

        if (!(aOther instanceof AgreementDiagnostic other)) {
            return false;
        }

        return type == other.type && Objects.equals(subject, other.subject);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, subject);
    }

    @Override
    public String toString()
    {
        return "AgreementDiagnostic [" + type + ": " + observation + "]";
    }
}
