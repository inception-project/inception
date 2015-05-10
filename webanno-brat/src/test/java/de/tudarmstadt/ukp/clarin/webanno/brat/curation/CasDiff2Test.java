/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.SpanDiffAdapter;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class CasDiff2Test
{
    @Test
    public void noDataTest()
        throws Exception
    {
        List<String> entryTypes = new ArrayList<>();
        
        List<DiffAdapter> diffAdapters = new ArrayList<>();

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());
    }

    @Test
    public void singleEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);
        
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
    }

    @Test
    public void twoEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);

        JCas user2Cas = JCasFactory.createJCas();
        user2Cas.setDocumentText(text);

        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(user1Cas));
        casByUser.put("user2", asList(user2Cas));

        List<String> entryTypes = asList(Token.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(new SpanDiffAdapter(Token.class.getName()));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(0, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName());
        
        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void noDifferencesPosDependencyTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/noDifferences/data.conll",
                "casdiff/noDifferences/data.conll");

        List<String> entryTypes = asList(POS.class.getName(), Dependency.class.getName());
        
        List<? extends DiffAdapter> diffAdapters = asList(
                SpanDiffAdapter.POS, 
                ArcDiffAdapter.DEPENDENCY);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(52, result.size());
        assertEquals(26, result.size(POS.class.getName()));
        assertEquals(26, result.size(Dependency.class.getName()));
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(1.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/singleSpanDifference/user1.conll",
                "casdiff/singleSpanDifference/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(1, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());

        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.0d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void someDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/someDifferences/user1.conll",
                "casdiff/someDifferences/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<SpanDiffAdapter> diffAdapters = asList(SpanDiffAdapter.POS);

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(4, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.836477987d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void singleNoDifferencesTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/singleSpanNoDifference/data.conll",
                "casdiff/singleSpanNoDifference/data.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(1, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(Double.NaN, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationDistanceTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/relationDistance/user1.conll",
                "casdiff/relationDistance/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(27, result.size());
        assertEquals(0, result.getDifferingConfigurations().size());
        assertEquals(2, result.getIncompleteConfigurations().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(1.0, agreement.getAgreement(), 0.000001d);
        assertEquals(2, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void spanLabelLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/spanLabel/user1.conll",
                "casdiff/spanLabel/user2.conll");

        List<String> entryTypes = asList(POS.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new SpanDiffAdapter(POS.class.getName(),
                "PosValue"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "PosValue", casByUser);
        assertEquals(0.958730d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    @Test
    public void relationLabelTest()
        throws Exception
    {
        Map<String, List<JCas>> casByUser = load(
                "casdiff/relationLabel/user1.conll",
                "casdiff/relationLabel/user2.conll");

        List<String> entryTypes = asList(Dependency.class.getName());

        List<? extends DiffAdapter> diffAdapters = asList(new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType"));

        DiffResult result = CasDiff2.doDiff(entryTypes, diffAdapters, casByUser);
        
        result.print(System.out);
        
        assertEquals(26, result.size());
        assertEquals(1, result.getDifferingConfigurations().size());
        assertEquals(0, result.getIncompleteConfigurations().size());
        
        AgreementResult agreement = AgreementUtils.getCohenKappaAgreement(result, entryTypes.get(0),
                "DependencyType", casByUser);
        assertEquals(0.958199d, agreement.getAgreement(), 0.000001d);
        assertEquals(0, agreement.getIncompleteSetsByPosition().size());
    }

    private static Map<String, List<JCas>> load(String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = read(path);
            casByUser.put("user"+n, asList(cas));
            n++;
        }
        return casByUser;
    }
    
    private static JCas read(String aPath)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        
        JCas jcas = JCasFactory.createJCas();
        
        reader.getNext(jcas.getCas());
        
        return jcas;
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
