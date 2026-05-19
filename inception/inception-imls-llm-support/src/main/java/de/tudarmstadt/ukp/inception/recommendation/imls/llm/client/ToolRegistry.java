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
 * Holds {@link ExecutableTool}s available for a given chat exchange. The caller composes the
 * registry from whatever tool sources apply (Java {@code @Tool} methods, MCP-discovered tools,
 * ad-hoc callbacks, ...) and looks tools up by name when a {@link ToolCall} is returned by the
 * model.
 * <p>
 * Tool names must be unique within a registry; registering a duplicate name fails fast.
 */
public interface ToolRegistry
{
    /**
     * Add a tool to the registry.
     *
     * @throws IllegalStateException
     *             if a tool with the same {@link ToolDescriptor#name() name} is already registered.
     */
    void register(ExecutableTool aTool);

    /**
     * Remove a tool by name. No-op if no tool with that name is registered.
     */
    void unregister(String aName);

    /**
     * @return the registered tool with the given name, or empty if none.
     */
    Optional<ExecutableTool> findByName(String aName);

    /**
     * @return all registered tools, in registration order.
     */
    Collection<ExecutableTool> all();

    /**
     * @return the wire-side {@link ToolDescriptor}s for all registered tools, in registration
     *         order. Suitable for passing into {@link ChatOptions#tools()}.
     */
    List<ToolDescriptor> toDescriptors();
}
