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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A short-lived collection of {@link ToolInvoker}s — typically composed for a single chat exchange
 * from whatever tool sources apply (Java {@code @Tool} methods, MCP-discovered tools, ad-hoc
 * callbacks, ...) and consulted by name when a {@link ToolCall} is returned by the model.
 * <p>
 * Tool names must be unique within a set; adding a duplicate name fails fast.
 */
public interface ToolInvokerSet
{
    /**
     * Add an invoker to the set.
     *
     * @throws IllegalStateException
     *             if an invoker with the same {@link ToolDescriptor#name() name} is already
     *             present.
     */
    void add(ToolInvoker aInvoker);

    /**
     * @return the invoker for the given tool name, or empty if none.
     */
    Optional<ToolInvoker> findByName(String aName);

    /**
     * @return all invokers, in insertion order.
     */
    Collection<ToolInvoker> all();

    /**
     * @return the wire-side {@link ToolDescriptor}s for all invokers, in insertion order. Suitable
     *         for passing into {@link ChatOptions#tools()}.
     */
    List<ToolDescriptor> toDescriptors();
}
