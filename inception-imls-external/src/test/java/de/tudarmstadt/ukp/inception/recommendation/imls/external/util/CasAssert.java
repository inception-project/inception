/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.external.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class CasAssert
    extends AbstractAssert<CasAssert, CAS>
{
    private static String TYPE_NE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";

    public CasAssert(CAS cas)
    {
        super(cas, CasAssert.class);
    }

    public CasAssert containsNamedEntity(String text, String value)
    {
        isNotNull();

        Type type = CasUtil.getType(actual, TYPE_NE);
        for (AnnotationFS annotation : CasUtil.select(actual, type)) {
            if (annotation.getCoveredText().equals(text) &&
                FSUtil.getFeature(annotation, "value", String.class).equals(value)) {
                return this;
            }
        }

        failWithMessage("No named entity with text <%s> and value <%s> found", text, value);

        return this;
    }

    public CasAssert containsPrediction(String text, String label)
    {
        isNotNull();

        Type type = CasUtil.getType(actual, PredictedSpan.class);
        for (AnnotationFS annotation : CasUtil.select(actual, type)) {
            if (annotation.getCoveredText().equals(text) &&
                FSUtil.getFeature(annotation, "label", String.class).equals(label)) {
                return this;
            }
        }

        failWithMessage("No named entity with text <%s> and label <%s> found", text, label);

        return this;
    }

    public ListAssert<AnnotationFS> extractNamedEntities()
    {
        isNotNull();

        Type type = CasUtil.getType(actual, TYPE_NE);
        List<AnnotationFS> result = new ArrayList<>(CasUtil.select(actual, type));
        return new ListAssert<>(result);
    }
}
