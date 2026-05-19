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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

class MethodToolTest
{
    static class Tools
    {
        boolean called;
        Object[] receivedArgs;

        @Tool(value = "echo", description = "Returns its input.")
        public String echo(@ToolParam(value = "text", description = "what to echo") String aText)
        {
            called = true;
            receivedArgs = new Object[] { aText };
            return aText;
        }

        @Tool(value = "add", description = "Adds two integers.")
        public int add(@ToolParam(value = "a", description = "first") int aA,
                @ToolParam(value = "b", description = "second") int aB)
        {
            called = true;
            receivedArgs = new Object[] { aA, aB };
            return aA + aB;
        }

        @Tool(value = "boom", description = "Throws.")
        public String boom(@ToolParam(value = "msg", description = "what to throw") String aMsg)
        {
            throw new IllegalArgumentException(aMsg);
        }

        @Tool(value = "needs_context", description = "Has an unannotated parameter.")
        public String needsContext(
                @ToolParam(value = "prefix", description = "prefix") String aPrefix,
                Object aRuntimeThing)
        {
            return aPrefix + aRuntimeThing;
        }
    }

    private static Method methodNamed(String aName)
    {
        for (var m : Tools.class.getDeclaredMethods()) {
            if (m.getName().equals(aName)) {
                return m;
            }
        }
        throw new AssertionError("No method " + aName);
    }

    @Test
    void descriptorIsBuiltFromMethod()
    {
        var sut = new MethodTool(new Tools(), methodNamed("echo"));
        assertThat(sut.descriptor().name()).isEqualTo("echo");
        assertThat(sut.descriptor().description()).contains("input");
        assertThat(sut.descriptor().parametersSchema()).isNotNull();
    }

    @Test
    void invokeBindsToolParamsFromArguments() throws Exception
    {
        var tools = new Tools();
        var sut = new MethodTool(tools, methodNamed("echo"));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("text", "hello");

        var result = sut.invoke(args);

        assertThat(result).isEqualTo("hello");
        assertThat(tools.called).isTrue();
        assertThat(tools.receivedArgs).containsExactly("hello");
    }

    @Test
    void invokeCoercesNumericTypesViaJackson() throws Exception
    {
        var tools = new Tools();
        var sut = new MethodTool(tools, methodNamed("add"));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("a", 2).put("b", 3);

        assertThat(sut.invoke(args)).isEqualTo(5);
    }

    @Test
    void constructionFailsOnUnannotatedParameter()
    {
        assertThatIllegalArgumentException() //
                .isThrownBy(() -> new MethodTool(new Tools(), methodNamed("needsContext"))) //
                .withMessageContaining("@ToolParam") //
                .withMessageContaining("ExecutableTool");
    }

    @Test
    void targetExceptionIsUnwrapped()
    {
        var sut = new MethodTool(new Tools(), methodNamed("boom"));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("msg", "kapow");

        assertThatThrownBy(() -> sut.invoke(args)) //
                .isInstanceOf(IllegalArgumentException.class) //
                .hasMessage("kapow");
    }
}
