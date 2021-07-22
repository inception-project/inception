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
package de.tudarmstadt.ukp.clarin.webanno.constraints.eval;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parseFile;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class ConstraintsGeneratorTest
{
    @Test
    public void testSimpleFeature() throws Exception
    {
        ParsedConstraints constraints = parseFile("src/test/resources/rules/9.rules");

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("is");

        Lemma lemma = new Lemma(jcas, 0, 2);
        lemma.setValue("be");
        lemma.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(lemma,
                "value", constraints);

        assertThat(possibleValues).containsExactly(new PossibleValue("be", true));
    }

    @Test
    public void testSimplePath() throws Exception
    {
        ParsedConstraints constraints = parseFile("src/test/resources/rules/10.rules");

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The sun.");

        // Add token annotations
        Token t_the = new Token(jcas, 0, 3);
        t_the.addToIndexes();
        Token t_sun = new Token(jcas, 0, 3);
        t_sun.addToIndexes();

        // Add POS annotations and link them to the tokens
        POS p_the = new POS(jcas, t_the.getBegin(), t_the.getEnd());
        p_the.setPosValue("DET");
        p_the.addToIndexes();
        t_the.setPos(p_the);
        POS p_sun = new POS(jcas, t_sun.getBegin(), t_sun.getEnd());
        p_sun.setPosValue("NN");
        p_sun.addToIndexes();
        t_sun.setPos(p_sun);

        // Add dependency annotations
        Dependency dep_the_sun = new Dependency(jcas);
        dep_the_sun.setGovernor(t_sun);
        dep_the_sun.setDependent(t_the);
        dep_the_sun.setDependencyType("det");
        dep_the_sun.setBegin(dep_the_sun.getGovernor().getBegin());
        dep_the_sun.setEnd(dep_the_sun.getGovernor().getEnd());
        dep_the_sun.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator
                .generatePossibleValues(dep_the_sun, "DependencyType", constraints);

        List<PossibleValue> expectedOutput = new LinkedList<>();
        expectedOutput.add(new PossibleValue("det", false));

        assertEquals(expectedOutput, possibleValues);
    }

    @Test
    public void testTwoConditions() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        CAS cas = jcas.getCas();

        List<Token> tokens = new ArrayList<>(select(jcas, Token.class));

        Token t1 = tokens.get(0);
        Token t2 = tokens.get(tokens.size() - 1);

        NamedEntity gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.setValue("Animal");
        gov.addToIndexes();
        NamedEntity dep = new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.setValue("NotWeight");
        dep.addToIndexes();

        Type relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        AnnotationFS fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        ParsedConstraints constraints = parseFile("src/test/resources/rules/twoConditions.rules");

        Evaluator constraintsEvaluator = new ValuesGenerator();
        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(fs1,
                "label", constraints);

        System.out.println(possibleValues);

        // "Weight" != "NotWeight", so the rule should not match
        assertEquals(0, possibleValues.size());
    }

    private JCas makeJCasOneSentence() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/types/webannoTestTypes.xml");

        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        JCas jcas = JCasFactory.createJCas(merged);

        DocumentMetaData.create(jcas).setDocumentId("doc");

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, "This is a test .");

        return jcas;
    }
}
