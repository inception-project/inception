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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

class ToolInvokerSetImplTest
{
    private static ToolInvoker stub(String aName)
    {
        return new ToolInvoker()
        {
            private final ToolDescriptor descriptor = new ToolDescriptor(aName, null, null);

            @Override
            public ToolDescriptor descriptor()
            {
                return descriptor;
            }

            @Override
            public Object invoke(JsonNode aArguments)
            {
                return aName;
            }

            @Override
            public String toString()
            {
                return "stub:" + aName;
            }
        };
    }

    @Test
    void emptySetHasNothing()
    {
        var sut = new ToolInvokerSetImpl();
        assertThat(sut.all()).isEmpty();
        assertThat(sut.toDescriptors()).isEmpty();
        assertThat(sut.findByName("anything")).isEmpty();
    }

    @Test
    void addAndFindByName()
    {
        var foo = stub("foo");
        var sut = new ToolInvokerSetImpl();
        sut.add(foo);

        assertThat(sut.findByName("foo")).containsSame(foo);
        assertThat(sut.findByName("bar")).isEmpty();
        assertThat(sut.findByName(null)).isEmpty();
    }

    @Test
    void duplicateNameFailsAndOriginalSurvives()
    {
        var first = stub("foo");
        var second = stub("foo");
        var sut = new ToolInvokerSetImpl();
        sut.add(first);

        assertThatIllegalStateException() //
                .isThrownBy(() -> sut.add(second)) //
                .withMessageContaining("foo");

        assertThat(sut.findByName("foo")).containsSame(first);
    }

    @Test
    void allAndToDescriptorsPreserveInsertionOrder()
    {
        var sut = new ToolInvokerSetImpl();
        sut.add(stub("alpha"));
        sut.add(stub("zulu"));
        sut.add(stub("mike"));

        assertThat(sut.all()).extracting(t -> t.descriptor().name()) //
                .containsExactly("alpha", "zulu", "mike");
        assertThat(sut.toDescriptors()).extracting(ToolDescriptor::name) //
                .containsExactly("alpha", "zulu", "mike");
    }

    @Test
    void constructorSeedsFromCollection()
    {
        var sut = new ToolInvokerSetImpl(List.of(stub("a"), stub("b")));
        assertThat(sut.toDescriptors()).extracting(ToolDescriptor::name) //
                .containsExactly("a", "b");
    }
}
