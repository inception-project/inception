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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link ToolRegistry} implementation backed by a {@link LinkedHashMap} keyed by tool name.
 * Not thread-safe; the typical use is per-request setup from a single thread.
 */
public class ToolRegistryImpl
    implements ToolRegistry
{
    private final Map<String, ExecutableTool> byName = new LinkedHashMap<>();

    public ToolRegistryImpl()
    {
    }

    public ToolRegistryImpl(Collection<? extends ExecutableTool> aTools)
    {
        if (aTools != null) {
            aTools.forEach(this::register);
        }
    }

    @Override
    public void register(ExecutableTool aTool)
    {
        var name = aTool.descriptor().name();
        var existing = byName.put(name, aTool);
        if (existing != null) {
            byName.put(name, existing); // restore — fail-fast wins
            throw new IllegalStateException("Duplicate tool registration for name [" + name + "]: ["
                    + existing + "] vs [" + aTool + "]");
        }
    }

    @Override
    public void unregister(String aName)
    {
        byName.remove(aName);
    }

    @Override
    public Optional<ExecutableTool> findByName(String aName)
    {
        if (aName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(aName));
    }

    @Override
    public Collection<ExecutableTool> all()
    {
        return Collections.unmodifiableCollection(byName.values());
    }

    @Override
    public List<ToolDescriptor> toDescriptors()
    {
        return byName.values().stream() //
                .map(ExecutableTool::descriptor) //
                .toList();
    }
}
