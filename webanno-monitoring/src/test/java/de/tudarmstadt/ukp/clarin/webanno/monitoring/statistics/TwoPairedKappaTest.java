/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.monitoring.statistics;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoCustomTsvReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

/**
 * Unit Test for Kappa Agreement. The example reads two TSV files with POS and DEP annotations for
 * two users and check the disagreement
 *
 * @author yimam
 */
public class TwoPairedKappaTest
{
    private User user1, user2, user3;
    private SourceDocument document;
    private CAS kappatestCas, kappaspandiff, kappaarcdiff, kappaspanarcdiff;

    @Before
    public void init()
        throws Exception
    {
        user1 = new User();
        user1.setUsername("user1");

        user2 = new User();
        user2.setUsername("user2");

        user3 = new User();
        user3.setUsername("user3");

        document = new SourceDocument();

        kappatestCas = JCasFactory.createJCas().getCas();
        CollectionReader reader1 = createReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "kappatest.tsv");
        reader1.getNext(kappatestCas);

        kappaspandiff = JCasFactory.createJCas().getCas();
        CollectionReader reader2 = createReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "kappaspandiff.tsv");
        reader2.getNext(kappaspandiff);

        kappaarcdiff = JCasFactory.createJCas().getCas();
        CollectionReader reader3 = createReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "kappaarcdiff.tsv");
        reader3.getNext(kappaarcdiff);

        kappaspanarcdiff = JCasFactory.createJCas().getCas();
        CollectionReader reader4 = createReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_SOURCE_LOCATION, "src/test/resources/",
                TcfReader.PARAM_PATTERNS, "kappaspanarcdiff.tsv");
        reader4.getNext(kappaspanarcdiff);
    }

    @Test
    public void testTwoUserSameAnnotation()
        throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        
        Map<User, JCas> userCases = new HashMap<>();
        userCases.put(user1, kappatestCas.getJCas());
        userCases.put(user2, kappatestCas.getJCas());
        
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<>();
        documentJCases.put(document, userCases);
        
        // Check against new impl
        DiffResult diff = CasDiff2.doDiff(POS.class, new SpanDiffAdapter(POS.class, "PosValue"),
                convert(userCases));
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, POS.class.getName(),
                "PosValue", convert(userCases));
        
        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        diff.print(System.out);
        
        assertEquals(1.0d, agreement.getAgreement(), 0.000001);
        assertEquals(9, diff.size());
        assertEquals(0, diff.getDifferingConfigurations().size());
        assertEquals(0, diff.getIncompleteConfigurations().size());
    }

    private Map<String, List<JCas>> convert(Map<User, JCas> aMap) {
        Map<String, List<JCas>> map = new LinkedHashMap<>();
        for (Entry<User, JCas> e : aMap.entrySet()) {
            map.put(e.getKey().getUsername(), asList(e.getValue()));
        }
        return map;
    }
    
    @Test
    public void testTwoUserDiffArcAnnotation()
        throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        
        Map<User, JCas> userCases = new HashMap<>();
        userCases.put(user1, kappatestCas.getJCas());
        userCases.put(user2, kappaarcdiff.getJCas());
        
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<>();
        documentJCases.put(document, userCases);
        
        // Check against new impl
        DiffResult diff = CasDiff2.doDiff(Dependency.class, new ArcDiffAdapter(Dependency.class,
                "Dependent", "Governor", "DependencyType"), convert(userCases));
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, Dependency.class.getName(),
                "DependencyType", convert(userCases));
        
        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        diff.print(System.out);
        
        assertEquals(0.86153d, agreement.getAgreement(), 0.00001d);
        assertEquals(9, diff.size());
        assertEquals(1, diff.getDifferingConfigurations().size());
        assertEquals(0, diff.getIncompleteConfigurations().size());
    }

    @Test
    public void testTwoUserDiffSpanAnnotation()
        throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, kappatestCas.getJCas());
        userCases.put(user2, kappaspandiff.getJCas());

        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<>();
        documentJCases.put(document, userCases);
        
        // Check against new impl
        DiffResult diff = CasDiff2.doDiff(POS.class, new SpanDiffAdapter(POS.class, "PosValue"),
                convert(userCases));
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, POS.class.getName(),
                "PosValue", convert(userCases));
        
        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        diff.print(System.out);
        
        assertEquals(0.86153d, agreement.getAgreement(), 0.00001d);
        assertEquals(9, diff.size());
        assertEquals(1, diff.getDifferingConfigurations().size());
        assertEquals(0, diff.getIncompleteConfigurations().size());
    }

    @Test
    public void testTwoUserDiffArcAndSpanAnnotation()
        throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, kappatestCas.getJCas());
        userCases.put(user2, kappaspanarcdiff.getJCas());

        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<>();
        documentJCases.put(document, userCases);
        
        // Check against new impl
        DiffResult diff = CasDiff2.doDiff(Dependency.class, new ArcDiffAdapter(Dependency.class,
                "Dependent", "Governor", "DependencyType"), convert(userCases));
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(diff, Dependency.class.getName(),
                "DependencyType", convert(userCases));
        
        // Asserts
        System.out.printf("Agreement: %s%n", agreement.toString());
        diff.print(System.out);
        AgreementUtils.dumpAgreementStudy(System.out, agreement);
        
        assertEquals(0.86153d, agreement.getAgreement(), 0.00001d);
        assertEquals(9, diff.size());
        assertEquals(1, diff.getDifferingConfigurations().size());
        assertEquals(0, diff.getIncompleteConfigurations().size());
    }

    @Test
    public void testThreeUserDiffArcAndSpanAnnotation()
        throws Exception
    {
        Map<User, List<SourceDocument>> userDocs = new HashMap<>();
        userDocs.put(user1, asList(document));
        userDocs.put(user2, asList(document));
        userDocs.put(user3, asList(document));
        
        Map<User, JCas> userCases = new HashMap<>();
        userCases.put(user1, kappatestCas.getJCas());
        userCases.put(user2, kappaspandiff.getJCas());
        userCases.put(user3, kappaspanarcdiff.getJCas());
        
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<>();
        documentJCases.put(document, userCases);

        // Check against new impl
        DiffResult diff = CasDiff2.doDiff(
                asList(POS.class.getName(), Dependency.class.getName()), 
                asList(SpanDiffAdapter.POS, ArcDiffAdapter.DEPENDENCY), 
                convert(userCases));
        
        Map<String, List<JCas>> user1and2 = convert(userCases);
        user1and2.remove("user3");
        AgreementResult agreement12 = AgreementUtils.getCohenKappaAgreement(diff,
                Dependency.class.getName(), "DependencyType", user1and2);

        Map<String, List<JCas>> user2and3 = convert(userCases);
        user2and3.remove("user1");
        AgreementResult agreement23 = AgreementUtils.getCohenKappaAgreement(diff,
                Dependency.class.getName(), "DependencyType", user2and3);

        Map<String, List<JCas>> user1and3 = convert(userCases);
        user1and3.remove("user2");
        AgreementResult agreement13 = AgreementUtils.getCohenKappaAgreement(diff,
                Dependency.class.getName(), "DependencyType", user1and3);

        // Asserts
        diff.print(System.out);
        
        System.out.printf("New agreement 1/2: %s%n", agreement12.toString());
        System.out.printf("New agreement 2/3: %s%n", agreement23.toString());
        System.out.printf("New agreement 1/3: %s%n", agreement13.toString());
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
