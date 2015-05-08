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

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class CasDiffTest
{
    @Test
    public void noDataTest()
        throws Exception
    {
        List<Type> entryTypes = new ArrayList<>();

        Map<String, JCas> casByUser = new LinkedHashMap<>();

        List<AnnotationOption> result = CasDiff.doDiff(entryTypes, casByUser, 0, 0);
        
        assertEquals(0, result.size());
    }

    @Test
    public void singleEmptyCasTest()
        throws Exception
    {
        String text = "";
        
        JCas user1Cas = JCasFactory.createJCas();
        user1Cas.setDocumentText(text);
        
        Map<String, JCas> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", user1Cas);

        List<Type> entryTypes = new ArrayList<>();
        entryTypes.add(JCasUtil.getType(user1Cas, Token.class));

        List<AnnotationOption> result = CasDiff.doDiff(entryTypes, casByUser, 0, text.length());
        
        assertEquals(0, result.size());
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

        Map<String, JCas> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", user1Cas);
        casByUser.put("user2", user2Cas);

        List<Type> entryTypes = new ArrayList<>();
        entryTypes.add(JCasUtil.getType(user1Cas, Token.class));

        List<AnnotationOption> result = CasDiff.doDiff(entryTypes, casByUser, 0, text.length());
        
        assertEquals(0, result.size());
    }

    @Test
    public void noDifferencesTest()
        throws Exception
    {
        JCas user1Cas = read("casdiff/noDifferences/data.conll");
        JCas user2Cas = read("casdiff/noDifferences/data.conll");

        Map<String, JCas> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", user1Cas);
        casByUser.put("user2", user2Cas);

        List<Type> entryTypes = new ArrayList<>();
        // Causes NPE
        // entryTypes.add(JCasUtil.getType(user1Cas, Token.class));
        entryTypes.add(JCasUtil.getType(user1Cas, POS.class));

        List<AnnotationOption> result = CasDiff.doDiff(entryTypes, casByUser, 0, user1Cas
                .getDocumentText().length());
        
        assertEquals(26, result.size());
        for (AnnotationOption opt : result) {
            for (AnnotationSelection sel : opt.getAnnotationSelections()) {
                System.out.println(sel.getAddressByUsername());
//                System.out.println(sel.getFsStringByUsername());
            }
        }
    }

    @Test
    public void singleDifferencesTest()
        throws Exception
    {
        JCas user1Cas = read("casdiff/singleSpanDifference/user1.conll");
        JCas user2Cas = read("casdiff/singleSpanDifference/user2.conll");

        Map<String, JCas> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", user1Cas);
        casByUser.put("user2", user2Cas);

        List<Type> entryTypes = new ArrayList<>();
        // Causes NPE
        // entryTypes.add(JCasUtil.getType(user1Cas, Token.class));
        entryTypes.add(JCasUtil.getType(user1Cas, POS.class));

        List<AnnotationOption> result = CasDiff.doDiff(entryTypes, casByUser, 0, user1Cas
                .getDocumentText().length());
        
        assertEquals(26, result.size());
        for (AnnotationOption opt : result) {
            for (AnnotationSelection sel : opt.getAnnotationSelections()) {
                System.out.println(sel.getAddressByUsername());
//                System.out.println(sel.getFsStringByUsername());
            }
        }
    }

    private JCas read(String aPath)
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
