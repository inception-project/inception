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
import static org.assertj.core.api.Assertions.entry;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.CLParse;

public class ImportVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        Map<String, String> imports = new LinkedHashMap<>();
        try (InputStream is = new FileInputStream("src/test/resources/rules/visitor-test.rules")) {
            ConstraintsGrammar parser = new ConstraintsGrammar(is, "UTF-8");
            CLParse p = parser.CLParse();
            p.accept(new ImportVisitor(), imports);
        }
        
        assertThat(imports).containsExactly(
                entry("Lemma", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma"),
                entry("Frame", "de.tudarmstadt.ukp.dkpro.core.api.semantics.Predicate"),
                entry("value", "pos.value"));
    }
}
