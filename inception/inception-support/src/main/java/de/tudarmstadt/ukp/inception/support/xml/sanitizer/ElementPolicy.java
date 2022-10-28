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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

public class ElementPolicy
{
    private final QName qName;
    private final ElementAction action;
    private final Map<QName, AttributePolicy> attributePolicies;

    public ElementPolicy(ElementAction aAction)
    {
        qName = null;
        action = aAction;
        attributePolicies = Collections.emptyMap();
    }

    public ElementPolicy(QName aQName, ElementAction aAction,
            Map<QName, AttributePolicy> aAttributePolicies)
    {
        qName = aQName;
        action = aAction;
        attributePolicies = aAttributePolicies;
    }

    public QName getQName()
    {
        return qName;
    }

    public ElementAction getAction()
    {
        return action;
    }

    public Optional<AttributePolicy> forAttribute(QName aElement, QName aAttributeUri,
            String aAttributeType, String aAttributeValue)
    {
        return Optional.ofNullable(attributePolicies.get(aAttributeUri));
    }

    public static ElementPolicy drop()
    {
        return new ElementPolicy(ElementAction.DROP);
    }

    public static ElementPolicy pass()
    {
        return new ElementPolicy(ElementAction.PASS);
    }
}
