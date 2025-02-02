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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

class CasWrapperTest
{
    private Jinjava jinjava;

    @BeforeEach
    void setup()
    {
        var config = new JinjavaConfig();
        jinjava = new Jinjava(config);
    }

    @Test
    void thatSelectCanAccessAnnotationsFromCas() throws Exception
    {
        var script = """
                {% for x in cas.select('NamedEntity') %}
                {{ x }}{% endfor %}""";

        var bindings = Map.of("test", "test");

        var cas = CasFactory.createCas();
        cas.setDocumentText("""
                My name is John McCain.
                His name is Mickey.""");
        buildAnnotation(cas, NamedEntity.class).on("John McCain").buildAndAddToIndexes();
        buildAnnotation(cas, NamedEntity.class).on("Mickey").buildAndAddToIndexes();

        jinjava.getGlobalContext().put("cas", new CasWrapper(cas));

        var result = jinjava.render(script, bindings);

        assertThat(result).contains("John McCain\nMickey");
    }
}
