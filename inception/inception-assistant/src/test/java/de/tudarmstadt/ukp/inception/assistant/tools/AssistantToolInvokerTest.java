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
package de.tudarmstadt.ukp.inception.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.CommandDispatcher;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

class AssistantToolInvokerTest
{
    static class Tools
    {
        Object[] received;

        @Tool(value = "echo", description = "Returns its input.")
        public String echo(@ToolParam(value = "text", description = "what to echo") String aText)
        {
            received = new Object[] { aText };
            return aText;
        }

        @Tool(value = "current_project", description = "Returns the current project name.")
        public String currentProject(Project aProject)
        {
            received = new Object[] { aProject };
            return aProject.getName();
        }

        @Tool(value = "current_document", description = "Returns the current document name.")
        public String currentDocument(SourceDocument aDocument)
        {
            received = new Object[] { aDocument };
            return aDocument.getName();
        }

        @Tool(value = "dispatch", description = "Has access to the command dispatcher.")
        public String dispatch(CommandDispatcher aDispatcher)
        {
            received = new Object[] { aDispatcher };
            return aDispatcher.getClass().getSimpleName();
        }

        @Tool(value = "editor", description = "Receives an AnnotationEditorContext.")
        public String editor(AnnotationEditorContext aEditorContext)
        {
            received = new Object[] { aEditorContext };
            return aEditorContext.getProject() != null ? aEditorContext.getProject().getName()
                    : "no-project";
        }

        @Tool(value = "mixed", description = "Mix of model args and runtime injection.")
        public String mixed(@ToolParam(value = "prefix", description = "prefix") String aPrefix,
                Project aProject)
        {
            received = new Object[] { aPrefix, aProject };
            return aPrefix + ":" + aProject.getName();
        }

        @Tool(value = "unsupported", description = "Declares an unsupported param.")
        public String unsupported(Object aOpaque)
        {
            return aOpaque.toString();
        }

        @Tool(value = "boom", description = "Throws.")
        public String boom(@ToolParam(value = "msg", description = "msg") String aMsg)
        {
            throw new IllegalArgumentException(aMsg);
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

    private static AssistantRuntimeContext ctx(Project aProject, SourceDocument aDocument,
            CommandDispatcher aDispatcher)
    {
        return new AssistantRuntimeContext(new User("alice"), aProject, aDocument, "alice",
                aDispatcher);
    }

    @Test
    void descriptorIsBuiltFromMethod()
    {
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("echo"), ctx(null, null, null));
        assertThat(sut.descriptor().name()).isEqualTo("echo");
        assertThat(sut.descriptor().description()).contains("input");
    }

    @Test
    void invokeBindsToolParamsFromArguments() throws Exception
    {
        var tools = new Tools();
        var sut = new AssistantToolInvoker(tools, methodNamed("echo"), ctx(null, null, null));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("text", "hello");

        assertThat(sut.invoke(args)).isEqualTo("hello");
        assertThat(tools.received).containsExactly("hello");
    }

    @Test
    void invokeInjectsProjectFromContext() throws Exception
    {
        var project = new Project();
        project.setName("demo");
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("currentProject"),
                ctx(project, null, null));

        assertThat(sut.invoke(null)).isEqualTo("demo");
    }

    @Test
    void invokeInjectsSourceDocumentFromContext() throws Exception
    {
        var doc = new SourceDocument();
        doc.setName("doc-1");
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("currentDocument"),
                ctx(null, doc, null));

        assertThat(sut.invoke(null)).isEqualTo("doc-1");
    }

    @Test
    void invokeInjectsCommandDispatcherFromContext() throws Exception
    {
        var dispatcher = mock(CommandDispatcher.class);
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("dispatch"),
                ctx(null, null, dispatcher));

        // Mockito gives back a generated subclass — its simple name reflects that.
        assertThat((String) sut.invoke(null)).startsWith("CommandDispatcher");
    }

    @Test
    void invokeBuildsAnnotationEditorContextFromRuntimeContext() throws Exception
    {
        var project = new Project();
        project.setName("ctx-project");
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("editor"),
                ctx(project, null, null));

        assertThat(sut.invoke(null)).isEqualTo("ctx-project");
    }

    @Test
    void invokeMixesToolParamsAndRuntimeInjection() throws Exception
    {
        var project = new Project();
        project.setName("mix");
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("mixed"),
                ctx(project, null, null));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("prefix", "hi");

        assertThat(sut.invoke(args)).isEqualTo("hi:mix");
    }

    @Test
    void unsupportedParameterTypeFailsWithClearMessage()
    {
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("unsupported"),
                ctx(null, null, null));

        assertThatIllegalStateException() //
                .isThrownBy(() -> sut.invoke(null)) //
                .withMessageContaining("unsupported") //
                .withMessageContaining(Object.class.getName());
    }

    @Test
    void targetExceptionIsUnwrapped()
    {
        var sut = new AssistantToolInvoker(new Tools(), methodNamed("boom"), ctx(null, null, null));
        var args = JSONUtil.getObjectMapper().createObjectNode().put("msg", "kapow");

        assertThatThrownBy(() -> sut.invoke(args)) //
                .isInstanceOf(IllegalArgumentException.class) //
                .hasMessage("kapow");
    }
}
