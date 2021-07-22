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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parseFile;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

public class SymbolicRulesTest
{
    @Test
    public void testSimpleSymbolicRules() throws Exception
    {
        ParsedConstraints constraints = parseFile("src/test/resources/rules/symbolic1.rules");

        JCas jcas = JCasFactory.createJCas();

        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/text/1.conll");

        reader.getNext(jcas.getCas());

        POS pos = new POS(jcas, 8, 9);
        pos.setPosValue("pronoun");
        pos.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        Lemma lemma = select(jcas, Lemma.class).iterator().next();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(lemma,
                "value", constraints);

        assertThat(possibleValues).containsExactly(new PossibleValue("good", true));
    }

    @Test
    public void testSimpleSymbolicRules2() throws Exception
    {
        ParsedConstraints constraints = parseFile("src/test/resources/rules/symbolic2.rules");

        JCas jcas = JCasFactory.createJCas();

        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/text/1.conll");

        reader.getNext(jcas.getCas());

        POS pos = new POS(jcas, 8, 9);
        pos.setPosValue("pronoun");
        pos.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        Lemma lemma = select(jcas, Lemma.class).iterator().next();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(lemma,
                "value", constraints);

        assertThat(possibleValues).containsExactly(new PossibleValue("good", true));
    }
}
