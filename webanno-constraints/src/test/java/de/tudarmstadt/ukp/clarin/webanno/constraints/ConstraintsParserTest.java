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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import java.io.FileInputStream;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;

public class ConstraintsParserTest
{
    @Test
    public void test1()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/1.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test2()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/2.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test3()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/3.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test4()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/4.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test5()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/5.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test6()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/6.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test7()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/7.rules"));
        Parse p = parser.Parse();
    }

    @Test
    public void test8()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/8.rules"));
        Parse p = parser.Parse();
    }

    /*
     * @Test public void testWithData() throws Exception { // BACKGROUND DATA JCas jcas =
     * JCasFactory.createJCas(); jcas.setDocumentText("Eva's");
     * 
     * Lemma l = new Lemma(jcas, 0,5); l.setValue("Eva"); l.addToIndexes();
     * 
     * Token t = new Token(jcas, 0,5); t.setLemma(l); t.addToIndexes();
     * 
     * // CONTEXT ANNOTATION WE SUPPOSE USER TO BE EDITING AND FOR WHICH WE NEED POSSIBLE VALUES
     * NamedEntity ne = new NamedEntity(jcas, 0,5); // ne.setValue("PERSON"); ne.addToIndexes();
     * 
     * String rules = "import "+Lemma.class+" as Lemma;\n" +
     * "@Lemma.value = \"Eva\" -> value = \"PERSON\"";
     * 
     * 
     * ConstraintsGrammar parser = new ConstraintsGrammar(new StringInputStream(rules)); Parse
     * parsedRules = parser.Parse();
     * 
     * List<String> possibleValues = extractPossibleValues(rules, ne, "value");
     * 
     * assertEquals(asList("PERSON"), possibleValues);
     * 
     * // The stuff below is supposedly in a completely different part of the code, so we assume
     * only // have access to rules, ne, and "value" (the args of extractPossibleValues)
     * 
     * String fqTypeName = resolveImport("Lemma");
     * 
     * CAS cas = ne.getCAS(); Type lemmaType = CasUtil.getType(ne.getCAS(), fqTypeName);
     * 
     * List<AnnotationFS> candidates = BratAjaxCasUtil.selectAt(cas, lemmaType, ne.getBegin(),
     * ne.getEnd()); String value = candidates.get(0).getFeatureValueAsString
     * (lemmaType.getFeatureByBaseName("value")); }
     */
}
