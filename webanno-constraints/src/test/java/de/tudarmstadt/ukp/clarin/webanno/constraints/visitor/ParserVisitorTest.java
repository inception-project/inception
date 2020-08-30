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

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLParse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;

public class ParserVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        ParsedConstraints constraints;
        try (InputStream is = new FileInputStream("src/test/resources/rules/visitor-test.rules")) {
            ConstraintsGrammar parser = new ConstraintsGrammar(is, "UTF-8");
            CLParse p = parser.CLParse();
            constraints = p.accept(new ParserVisitor());
        }
        
        System.out.printf("%s %n", constraints);
    }
}
