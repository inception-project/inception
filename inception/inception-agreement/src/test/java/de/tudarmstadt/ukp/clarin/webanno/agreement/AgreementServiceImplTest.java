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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.CodingStudyUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl.NER_DIFF_ADAPTER;
import static java.util.Arrays.asList;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder;

class AgreementServiceImplTest
{
    private static final String NULL_LABEL = "<<NONE>>";

    @SuppressWarnings("unchecked")
    @Test
    void testIncompletePosition() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", null, asList("PER")), //
                new Row(",") //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],\"INCOMPLETE_POSITION, USED\",<no annotation>,PER");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testStackedPosition() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER", "LOC")), //
                new Row(",") //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],STACKED,\"LOC, PER\",<no annotation>");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testStackedWithDifference() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER", "LOC"), asList("ORG")), //
                new Row(",") //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],STACKED,\"LOC, PER\",ORG");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCompleteWithOneNullLabel() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER"), asList(NULL_LABEL)), //
                new Row(","), //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],\"DIFFERENCE, COMPLETE, USED\",PER,<no label>");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCompleteWithTwoNullLabel() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList(NULL_LABEL), asList(NULL_LABEL)), //
                new Row(","), //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],\"COMPLETE, USED\",<no label>,<no label>");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCompleteAgreement() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER"), asList("PER")), //
                new Row(","), //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],\"COMPLETE, USED\",PER,PER");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCompleteDisagreement() throws Exception
    {
        var userCount = 2;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER"), asList("LOC")), //
                new Row(","), //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity," //
                        + "value,8-12 [John],\"DIFFERENCE, COMPLETE, USED\",PER,LOC");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMixed() throws Exception
    {
        var userCount = 3;
        Row[] data = { //
                new Row("This"), //
                new Row("is"), //
                new Row("John", asList("PER"), asList("LOC"), asList(NULL_LABEL)), //
                new Row(","), //
        };

        assertThat(generateReport(userCount, data).lines()).contains( //
                "SpanPosition,,,de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity,"
                        + "value,8-12 [John],\"DIFFERENCE, COMPLETE, USED\",PER,LOC,<no label>");
    }

    private String generateReport(int aUserCount, Row[] data) throws Exception, IOException
    {
        var type = NamedEntity._TypeName;
        var feature = NamedEntity._FeatName_value;
        var userCases = convert(aUserCount, data, type, feature);
        var diff = doDiff(asList(NER_DIFF_ADAPTER), userCases);
        Set<String> tagSet = null;
        var agreementResult = makeCodingStudy(diff, type, feature, tagSet, false, userCases);

        var buffer = new StringBuilder();
        try (var printer = new CSVPrinter(buffer, RFC4180)) {
            AgreementServiceImpl.configurationSetsWithItemsToCsv(printer, agreementResult, true);
        }
        return buffer.toString();
    }

    Map<String, CAS> convert(int aUserCount, Row[] aData, String aType, String aFeature)
        throws Exception
    {
        var userCases = new HashMap<String, CAS>();
        for (int i = 0; i < aUserCount; i++) {
            userCases.put("user" + (i + 1), CasFactory.createCas());
        }

        var text = new StringBuilder();
        for (var row : aData) {
            if (!text.isEmpty()) {
                text.append(" ");
            }

            var word = row.text;

            int i = 1;
            for (var user : row.users) {
                var cas = userCases.get("user" + i);
                convert(cas, aType, aFeature, text.length(), text.length() + word.length(), user);
                i++;
            }

            text.append(word);
        }

        userCases.values().forEach(cas -> cas.setDocumentText(text.toString()));

        return userCases;
    }

    void convert(CAS aCas, String aType, String aFeature, int aBegin, int aEnd, List<String> aData)
    {
        if (aData == null) {
            return;
        }

        for (var tag : aData) {
            AnnotationBuilder.buildAnnotation(aCas, aType) //
                    .at(aBegin, aEnd) //
                    .withFeature(aFeature, NULL_LABEL.equals(tag) ? null : tag) //
                    .buildAndAddToIndexes();
        }
    }

    @SuppressWarnings("unchecked")
    record Row(String text, List<String>... users) {};
}
