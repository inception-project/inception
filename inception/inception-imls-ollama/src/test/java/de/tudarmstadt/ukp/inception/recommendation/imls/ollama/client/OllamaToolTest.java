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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImplTest.AnnotationSpec;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImplTest.ComplexToolService;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImplTest.SimpleToolService;

public class OllamaToolTest
{
    @Test
    void testForMethod_simpleParameters() throws Exception
    {
        var service = new SimpleToolService();
        var method = service.getClass().getMethod("greet", String.class);

        var tool = OllamaTool.forMethod(service, method);

        assertThat(tool).isNotNull();
        assertThat(tool.getType()).isEqualTo("function");
        assertThat(tool.getFunction()).isNotNull();
        assertThat(tool.getFunction().getName()).isEqualTo("greet");
        assertThat(tool.getFunction().getDescription()).contains("Greets a person");

        var parameters = tool.getFunction().getParameters();
        assertThat(parameters.getProperties()).containsKey("name");
        assertThat(parameters.getRequired()).contains("name");
    }

    @Test
    void testForMethod_complexParameter() throws Exception
    {
        var service = new ComplexToolService();
        var method = service.getClass().getMethod("createAnnotation", AnnotationSpec.class);

        var tool = OllamaTool.forMethod(service, method);

        assertThat(tool.getFunction().getName()).isEqualTo("create_annotation");
        assertThat(tool.getFunction().getDescription()).contains("Creates annotation");

        var parameters = tool.getFunction().getParameters();
        assertThat(parameters.getProperties()).containsKey("spec");
        assertThat(parameters.getRequired()).contains("spec");

        // Verify the complex parameter schema includes nested properties
        var specProperty = parameters.getProperties().get("spec");
        assertThat(specProperty).isNotNull();
        assertThat(specProperty.has("properties")).isTrue();

        var specProperties = specProperty.get("properties");
        assertThat(specProperties.has("begin")).isTrue();
        assertThat(specProperties.has("end")).isTrue();
        assertThat(specProperties.has("label")).isTrue();
    }

    @Test
    void testForMethod_listOfComplexParameters() throws Exception
    {
        var service = new ComplexToolService();
        var method = service.getClass().getMethod("createMultipleAnnotations", List.class);

        var tool = OllamaTool.forMethod(service, method);

        assertThat(tool.getFunction().getName()).isEqualTo("create_multiple");

        var parameters = tool.getFunction().getParameters();
        assertThat(parameters.getProperties()).containsKey("specs");
        assertThat(parameters.getRequired()).contains("specs");

        // Verify it's an array type
        var specsProperty = parameters.getProperties().get("specs");
        assertThat(specsProperty.has("type")).isTrue();
        assertThat(specsProperty.get("type").asText()).isEqualTo("array");

        // Verify array items have the correct complex type
        assertThat(specsProperty.has("items")).isTrue();
        var items = specsProperty.get("items");
        assertThat(items.has("properties")).isTrue();

        var itemProperties = items.get("properties");
        assertThat(itemProperties.has("begin")).isTrue();
        assertThat(itemProperties.has("end")).isTrue();
        assertThat(itemProperties.has("label")).isTrue();
    }

}
