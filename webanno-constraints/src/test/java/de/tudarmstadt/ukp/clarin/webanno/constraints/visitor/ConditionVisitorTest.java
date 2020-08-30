/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLParse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;

public class ConditionVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        List<Condition> conditions = new ArrayList<>();
        try (InputStream is = new FileInputStream("src/test/resources/rules/visitor-test.rules")) {
            ConstraintsGrammar parser = new ConstraintsGrammar(is, "UTF-8");
            CLParse p = parser.CLParse();
            p.accept(new ConditionVisitor(), conditions);
        }

        assertThat(conditions).containsExactly(
                new Condition("@Lemma.value", "jump"),
                new Condition("frame", "Jumping"),
                new Condition("@access", "open"),
                new Condition("location", "parent"));
    }
}
