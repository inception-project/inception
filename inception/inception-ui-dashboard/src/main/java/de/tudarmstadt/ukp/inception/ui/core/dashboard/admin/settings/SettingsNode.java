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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import static java.util.Comparator.comparing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One node in the namespace tree of {@link ConfigurationProperty configuration properties}. The
 * root node has an empty path; every other node represents a dot-segment of the property hierarchy.
 * Properties are attached to the node corresponding to their parent namespace; pure namespace nodes
 * (no own properties) have only children.
 */
public class SettingsNode
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String path;
    private final String name;
    private final List<SettingsNode> children = new ArrayList<>();
    private final List<ConfigurationProperty> properties = new ArrayList<>();

    private SettingsNode(String aPath, String aName)
    {
        path = aPath;
        name = aName;
    }

    public String getPath()
    {
        return path;
    }

    public String getName()
    {
        return name;
    }

    public List<SettingsNode> getChildren()
    {
        return children;
    }

    public List<ConfigurationProperty> getProperties()
    {
        return properties;
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }
        if (!(aOther instanceof SettingsNode other)) {
            return false;
        }
        return Objects.equals(path, other.path);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(path);
    }

    /**
     * Build a tree from the flat list of properties. Returns the synthetic root node whose path is
     * empty and whose children are the top-level namespaces.
     */
    public static SettingsNode buildTree(List<ConfigurationProperty> aProperties)
    {
        var root = new SettingsNode("", "");
        // Build with mutable maps so we can merge as we go, then snapshot to lists.
        var byPath = new LinkedHashMap<String, NodeBuilder>();
        byPath.put("", new NodeBuilder(root));

        for (var property : aProperties) {
            var name = property.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            var segments = name.split("\\.");
            // Walk/create nodes for every prefix except the last segment, which is the
            // property's own name and should not become a tree node.
            var parentBuilder = byPath.get("");
            var pathSoFar = new StringBuilder();
            for (var i = 0; i < segments.length - 1; i++) {
                if (i > 0) {
                    pathSoFar.append('.');
                }
                pathSoFar.append(segments[i]);
                var path = pathSoFar.toString();
                var child = byPath.get(path);
                if (child == null) {
                    var node = new SettingsNode(path, segments[i]);
                    child = new NodeBuilder(node);
                    byPath.put(path, child);
                    parentBuilder.children.put(segments[i], child);
                }
                parentBuilder = child;
            }
            parentBuilder.node.properties.add(property);
        }

        // Materialize: copy sorted child lists onto each node, sort properties by name.
        for (var builder : byPath.values()) {
            var sortedChildren = new ArrayList<>(builder.children.values());
            sortedChildren.sort(comparing(b -> b.node.name));
            for (var child : sortedChildren) {
                builder.node.children.add(child.node);
            }
            builder.node.properties.sort(comparing(ConfigurationProperty::getName));
        }

        return root;
    }

    private static final class NodeBuilder
    {
        final SettingsNode node;
        final Map<String, NodeBuilder> children = new LinkedHashMap<>();

        NodeBuilder(SettingsNode aNode)
        {
            node = aNode;
        }
    }
}
