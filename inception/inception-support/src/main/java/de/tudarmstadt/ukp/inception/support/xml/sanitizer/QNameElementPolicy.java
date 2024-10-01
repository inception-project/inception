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

public class QNameElementPolicy
{
    private final QName qName;
    private final ElementAction action;
    private final Map<QName, QNameAttributePolicy> attributePolicies;

    public QNameElementPolicy(ElementAction aAction)
    {
        qName = null;
        action = aAction;
        attributePolicies = Collections.emptyMap();
    }

    public QNameElementPolicy(QName aQName, ElementAction aAction,
            Map<QName, QNameAttributePolicy> aAttributePolicies)
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

    public Optional<QNameAttributePolicy> forAttribute(QName aElement, QName aAttributeUri,
            String aAttributeType, String aAttributeValue)
    {
        return Optional.ofNullable(attributePolicies.get(aAttributeUri));
    }

    public static QNameElementPolicy drop()
    {
        return new QNameElementPolicy(ElementAction.DROP);
    }

    public static QNameElementPolicy skip()
    {
        return new QNameElementPolicy(ElementAction.SKIP);
    }

    public static QNameElementPolicy prune()
    {
        return new QNameElementPolicy(ElementAction.PRUNE);
    }

    public static QNameElementPolicy pass()
    {
        return new QNameElementPolicy(ElementAction.PASS);
    }

    @Override
    public String toString()
    {
        return "[" + qName + " -> " + action + "]";
    }
}
