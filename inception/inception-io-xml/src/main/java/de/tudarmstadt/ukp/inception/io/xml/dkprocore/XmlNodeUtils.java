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
package de.tudarmstadt.ukp.inception.io.xml.dkprocore;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;

import java.util.ArrayList;
import java.util.Optional;

import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;

public class XmlNodeUtils
{
    public static boolean hasAttributeWithValue(XmlElement e, String aAttribute, String aValue)
    {
        return getAttributeValue(e, aAttribute).map(v -> aValue.equals(v)).orElse(false);
    }

    public static Optional<String> getAttributeValue(XmlElement aElement, String aString)
    {
        var attributes = aElement.getAttributes();

        if (attributes == null) {
            return Optional.empty();
        }

        return attributes.select(XmlAttribute.class) //
                .filter(a -> aString.equals(a.getQName())) //
                .findFirst() //
                .map(XmlAttribute::getValue);
    }

    public static void removeFromTree(XmlElement aElement)
    {
        ArrayList<XmlNode> newSiblings = new ArrayList<>();
        var parent = aElement.getParent();
        var siblings = parent.getChildren().toArray(new XmlNode[parent.getChildren().size()]);
        var i = indexOf(siblings, aElement);
        asList(subarray(siblings, 0, i)).forEach(newSiblings::add);
        aElement.getChildren().forEach(child -> {
            child.setParent(parent);
            newSiblings.add(child);
        });
        asList(subarray(siblings, i + 1, siblings.length)).forEach(newSiblings::add);
        parent.setChildren(createFSArray(aElement.getJCas(), newSiblings));
        aElement.removeFromIndexes();
    }
}
