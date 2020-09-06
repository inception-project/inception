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
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;

public class RuleVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        List<Rule> rules = new ArrayList<>();
        try (InputStream is = new FileInputStream("src/test/resources/rules/visitor-test.rules")) {
            ConstraintsGrammar parser = new ConstraintsGrammar(is, "UTF-8");
            Parse p = parser.Parse();
            p.accept(new RuleVisitor(), rules);
        }
        
        for (Rule rule : rules) {
            System.out.printf("%s %n", rule);
        }
        
        assertThat(rules).hasSize(3);
    }
}
