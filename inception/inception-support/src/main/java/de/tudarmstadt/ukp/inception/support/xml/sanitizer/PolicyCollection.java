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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

public class PolicyCollection
{
    private final Map<QName, ElementPolicy> elementPolicies;
    private final Map<QName, AttributePolicy> globalAttributePolicies;
    private final ElementAction defaultElementAction;
    private final AttributeAction defaultAttributeAction;

    private boolean debug = false;

    private String name;
    private String version;

    public PolicyCollection(Map<QName, ElementPolicy> aElementPolicies,
            Map<QName, AttributePolicy> aGlobalAttributePolicies,
            ElementAction aDefaultElementAction, AttributeAction aDefaultAttributeAction)
    {
        elementPolicies = aElementPolicies;
        globalAttributePolicies = aGlobalAttributePolicies;
        defaultElementAction = aDefaultElementAction;
        defaultAttributeAction = aDefaultAttributeAction;
    }

    public Optional<ElementPolicy> forElement(QName aElement)
    {
        return Optional.ofNullable(elementPolicies.get(aElement));
    }

    public Optional<AttributeAction> forAttribute(QName aElement, QName aAttribute,
            String aAttributeType, String aAttributeValue)
    {
        var elementPolicy = elementPolicies.get(aElement);

        if (elementPolicy != null) {
            var attributePolicy = elementPolicy.forAttribute(aElement, aAttribute, aAttributeType,
                    aAttributeValue);

            var attributeAction = attributePolicy.flatMap(p -> p.apply(aAttributeValue));

            if (attributeAction.isPresent()) {
                return attributeAction;
            }
        }

        return Optional.ofNullable(globalAttributePolicies.get(aAttribute))
                .flatMap(p -> p.apply(aAttributeValue));
    }

    public Collection<ElementPolicy> getElementPolicies()
    {
        return Collections.unmodifiableCollection(elementPolicies.values());
    }

    public Collection<AttributePolicy> getGlobalAttributeElementPolicies()
    {
        return Collections.unmodifiableCollection(globalAttributePolicies.values());
    }

    public AttributeAction getDefaultAttributeAction()
    {
        return defaultAttributeAction;
    }

    public ElementAction getDefaultElementAction()
    {
        return defaultElementAction;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug(boolean aDebug)
    {
        debug = aDebug;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String aVersion)
    {
        version = aVersion;
    }
}
