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
package de.tudarmstadt.ukp.inception.io.bioc.model;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class BioCRelation
    extends BioCObject
{
    private String id;
    private List<BioCNode> nodes;

    public String getId()
    {
        return id;
    }

    @XmlAttribute(name = "id")
    public void setId(String aId)
    {
        id = aId;
    }

    public List<BioCNode> getNodes()
    {
        return nodes;
    }

    @XmlElement(name = "node")
    public void setNodes(List<BioCNode> aNodes)
    {
        nodes = aNodes;
    }

    public void addNode(String aRole, String aRefId)
    {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(new BioCNode(aRefId, aRole));
    }

    public Map<String, String> nodeMap()
    {
        return nodes.stream().collect(toMap(BioCNode::getRole, BioCNode::getRefId));
    }
}
