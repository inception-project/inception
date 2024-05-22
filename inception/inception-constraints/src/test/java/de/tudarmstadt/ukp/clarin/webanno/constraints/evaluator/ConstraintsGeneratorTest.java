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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parseFile;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

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
        var constraints = parseFile("src/test/resources/rules/9.rules");

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("is");

        var lemma = new Lemma(jcas, 0, 2);
        lemma.setValue("be");
        lemma.addToIndexes();

        var constraintsEvaluator = new ConstraintsEvaluator();

        var possibleValues = constraintsEvaluator.generatePossibleValues(constraints, lemma,
                "value");

        assertThat(possibleValues).containsExactly(new PossibleValue("be", true));
    }

    @Test
    public void testSimplePath() throws Exception
    {
        var constraints = parseFile("src/test/resources/rules/10.rules");

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The sun.");

        // Add token annotations
        var t_the = new Token(jcas, 0, 3);
        t_the.addToIndexes();
        var t_sun = new Token(jcas, 0, 3);
        t_sun.addToIndexes();

        // Add POS annotations and link them to the tokens
        var p_the = new POS(jcas, t_the.getBegin(), t_the.getEnd());
        p_the.setPosValue("DET");
        p_the.addToIndexes();
        t_the.setPos(p_the);
        var p_sun = new POS(jcas, t_sun.getBegin(), t_sun.getEnd());
        p_sun.setPosValue("NN");
        p_sun.addToIndexes();
        t_sun.setPos(p_sun);

        // Add dependency annotations
        var dep_the_sun = new Dependency(jcas);
        dep_the_sun.setGovernor(t_sun);
        dep_the_sun.setDependent(t_the);
        dep_the_sun.setDependencyType("det");
        dep_the_sun.setBegin(dep_the_sun.getGovernor().getBegin());
        dep_the_sun.setEnd(dep_the_sun.getGovernor().getEnd());
        dep_the_sun.addToIndexes();

        var constraintsEvaluator = new ConstraintsEvaluator();

        var possibleValues = constraintsEvaluator.generatePossibleValues(constraints, dep_the_sun,
                "DependencyType");

        var expectedOutput = new LinkedList<>();
        expectedOutput.add(new PossibleValue("det", false));

        assertEquals(expectedOutput, possibleValues);
    }

    @Test
    public void testTwoConditions() throws Exception
    {
        var jcas = makeJCasOneSentence();
        var cas = jcas.getCas();

        var tokens = new ArrayList<>(select(jcas, Token.class));

        var t1 = tokens.get(0);
        var t2 = tokens.get(tokens.size() - 1);

        var gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.setValue("Animal");
        gov.addToIndexes();
        var dep = new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.setValue("NotWeight");
        dep.addToIndexes();

        var relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        var fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        var constraints = parseFile("src/test/resources/rules/twoConditions.rules");

        var constraintsEvaluator = new ConstraintsEvaluator();
        var possibleValues = constraintsEvaluator.generatePossibleValues(constraints, fs1, "label");

        // System.out.println(possibleValues);

        // "Weight" != "NotWeight", so the rule should not match
        assertEquals(0, possibleValues.size());
    }

    private JCas makeJCasOneSentence() throws UIMAException
    {
        var global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                "src/test/resources/desc/types/webannoTestTypes.xml");

        var merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        var jcas = JCasFactory.createJCas(merged);

        DocumentMetaData.create(jcas).setDocumentId("doc");

        var tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, "This is a test .");

        return jcas;
    }
}
