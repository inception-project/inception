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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementTestUtils.getCohenKappaAgreement;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.DISAGREE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState.calculateState;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffAdapterImpl.POS_DIFF_ADAPTER;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv2Reader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Unit Test for Kappa Agreement. The example reads two TSV files with POS and DEP annotations for
 * two users and check the disagreement
 *
 */
public class TwoPairedKappaTest
{
    private User user1, user2, user3;
    private SourceDocument document;
    private CAS kappatestCas, kappaspandiff, kappaarcdiff, kappaspanarcdiff;

    @BeforeEach
    public void init() throws Exception
    {
        user1 = User.builder().withUsername("user1").build();
        user2 = User.builder().withUsername("user2").build();
        user3 = User.builder().withUsername("user3").build();
        document = new SourceDocument();

        kappatestCas = JCasFactory.createJCas().getCas();
        var reader1 = createReader( //
                WebannoTsv2Reader.class, //
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/", //
                WebannoTsv2Reader.PARAM_PATTERNS, "kappatest.tsv");
        reader1.getNext(kappatestCas);

        kappaspandiff = JCasFactory.createJCas().getCas();
        var reader2 = createReader( //
                WebannoTsv2Reader.class, //
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/", //
                WebannoTsv2Reader.PARAM_PATTERNS, "kappaspandiff.tsv");
        reader2.getNext(kappaspandiff);

        kappaarcdiff = JCasFactory.createJCas().getCas();
        var reader3 = createReader( //
                WebannoTsv2Reader.class, //
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/", //
                WebannoTsv2Reader.PARAM_PATTERNS, "kappaarcdiff.tsv");
        reader3.getNext(kappaarcdiff);

        kappaspanarcdiff = JCasFactory.createJCas().getCas();
        var reader4 = createReader( //
                WebannoTsv2Reader.class, //
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/", //
                WebannoTsv2Reader.PARAM_PATTERNS, "kappaspanarcdiff.tsv");
        reader4.getNext(kappaspanarcdiff);
    }

    @Test
    public void testTwoUserSameAnnotation() throws Exception
    {
        var userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));

        var userCases = new HashMap<User, CAS>();
        userCases.put(user1, kappatestCas);
        userCases.put(user2, kappatestCas);

        var documentJCases = new HashMap<SourceDocument, Map<User, CAS>>();
        documentJCases.put(document, userCases);

        // Check against new impl
        var diff = doDiff(asList(POS_DIFF_ADAPTER), convert(userCases));
        var result = diff.toResult();
        var agreement = getCohenKappaAgreement(diff, POS.class.getName(), "PosValue",
                convert(userCases));

        // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // result.print(System.out);

        assertThat(agreement.getAgreement()).isCloseTo(1.0d, within(0.00001d));
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.getDifferingConfigurationSets()).isEmpty();
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(AGREE);
    }

    private Map<String, CAS> convert(Map<User, CAS> aMap)
    {
        var map = new LinkedHashMap<String, CAS>();
        for (var e : aMap.entrySet()) {
            map.put(e.getKey().getUsername(), e.getValue());
        }
        return map;
    }

    @Test
    public void testTwoUserDiffArcAnnotation() throws Exception
    {
        var userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));

        var userCases = new HashMap<User, CAS>();
        userCases.put(user1, kappatestCas);
        userCases.put(user2, kappaarcdiff);

        var documentJCases = new HashMap<SourceDocument, Map<User, CAS>>();
        documentJCases.put(document, userCases);

        // Check against new impl
        var diff = doDiff(asList(DEPENDENCY_DIFF_ADAPTER), convert(userCases));
        var result = diff.toResult();
        var agreement = getCohenKappaAgreement(diff, Dependency.class.getName(), "DependencyType",
                convert(userCases));

        // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // result.print(System.out);

        assertThat(agreement.getAgreement()).isCloseTo(0.86153d, within(0.00001d));
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Test
    public void testTwoUserDiffSpanAnnotation() throws Exception
    {
        var userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));

        var userCases = new HashMap<User, CAS>();
        userCases.put(user1, kappatestCas);
        userCases.put(user2, kappaspandiff);

        var documentJCases = new HashMap<SourceDocument, Map<User, CAS>>();
        documentJCases.put(document, userCases);

        // Check against new impl
        var diff = doDiff(asList(POS_DIFF_ADAPTER), convert(userCases));
        var result = diff.toResult();
        var agreement = getCohenKappaAgreement(diff, POS.class.getName(), "PosValue",
                convert(userCases));

        // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // result.print(System.out);

        assertThat(agreement.getAgreement()).isCloseTo(0.86153d, within(0.00001d));
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Test
    public void testTwoUserDiffArcAndSpanAnnotation() throws Exception
    {
        var userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));

        var userCases = new HashMap<User, CAS>();
        userCases.put(user1, kappatestCas);
        userCases.put(user2, kappaspanarcdiff);

        var documentJCases = new HashMap<SourceDocument, Map<User, CAS>>();
        documentJCases.put(document, userCases);

        // Check against new impl
        var diff = doDiff(asList(DEPENDENCY_DIFF_ADAPTER), convert(userCases));
        var result = diff.toResult();
        var agreement = getCohenKappaAgreement(diff, Dependency.class.getName(), "DependencyType",
                convert(userCases));

        // Asserts
        // System.out.printf("Agreement: %s%n", agreement.toString());
        // result.print(System.out);
        // AgreementUtils.dumpAgreementStudy(System.out, agreement);

        assertThat(agreement.getAgreement()).isCloseTo(0.86153d, within(0.00001d));
        assertThat(result.size()).isEqualTo(9);
        assertThat(result.getDifferingConfigurationSets()).hasSize(1);
        assertThat(result.getIncompleteConfigurationSets()).isEmpty();
        assertThat(calculateState(result)).isEqualTo(DISAGREE);
    }

    @Test
    public void testThreeUserDiffArcAndSpanAnnotation() throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        userDocs.put(user3, asList(document));

        var userCases = new HashMap<User, CAS>();
        userCases.put(user1, kappatestCas);
        userCases.put(user2, kappaspandiff);
        userCases.put(user3, kappaspanarcdiff);

        var documentJCases = new HashMap<SourceDocument, Map<User, CAS>>();
        documentJCases.put(document, userCases);

        // Check against new impl
        var diff = doDiff(asList(POS_DIFF_ADAPTER, DEPENDENCY_DIFF_ADAPTER), convert(userCases));
        DiffResult result = diff.toResult();

        var user1and2 = convert(userCases);
        user1and2.remove("user3");
        var agreement12 = getCohenKappaAgreement(diff, Dependency.class.getName(), "DependencyType",
                user1and2);

        var user2and3 = convert(userCases);
        user2and3.remove("user1");
        var agreement23 = getCohenKappaAgreement(diff, Dependency.class.getName(), "DependencyType",
                user2and3);

        var user1and3 = convert(userCases);
        user1and3.remove("user2");
        var agreement13 = getCohenKappaAgreement(diff, Dependency.class.getName(), "DependencyType",
                user1and3);

        // Asserts
        // result.print(System.out);

        // System.out.printf("New agreement 1/2: %s%n", agreement12.toString());
        // System.out.printf("New agreement 2/3: %s%n", agreement23.toString());
        // System.out.printf("New agreement 1/3: %s%n", agreement13.toString());
    }
}
