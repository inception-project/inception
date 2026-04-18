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

import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

public class AttributePolicyBuilder
{
    private final PolicyCollectionBuilder parent;
    private final Pattern[] qnamePatterns;
    private final QName[] attributeNames;
    private final AttributeAction action;

    private Pattern valuePattern;

    public AttributePolicyBuilder(PolicyCollectionBuilder aParent, AttributeAction aAction,
            Pattern[] aQNamePattern)
    {
        parent = aParent;
        qnamePatterns = aQNamePattern;
        attributeNames = null;
        action = aAction;
    }

    public AttributePolicyBuilder(PolicyCollectionBuilder aParent, AttributeAction aAction,
            QName... aAttributeNames)
    {
        parent = aParent;
        qnamePatterns = null;
        attributeNames = aAttributeNames;
        action = aAction;
    }

    public AttributePolicyBuilder matchingValue(Pattern aPattern)
    {
        if (aPattern == null) {
            throw new IllegalArgumentException("matching requires a pattern");
        }

        valuePattern = aPattern;
        return this;
    }

    public PolicyCollectionBuilder globally()
    {
        if (qnamePatterns != null) {
            var policy = makePolicy(qnamePatterns);
            parent.globalAttributePolicy(policy);
            return parent;
        }

        for (var attributeName : attributeNames) {
            var policy = makePolicy(attributeName);
            parent.globalAttributePolicy(policy);
        }

        return parent;
    }

    public PolicyCollectionBuilder onElements(String... aElementNames)
    {
        var elementNames = Stream.of(aElementNames).map(QName::new).toArray(QName[]::new);
        return onElements(elementNames);
    }

    public PolicyCollectionBuilder onElements(QName... aElementNames)
    {
        if (aElementNames.length == 0) {
            throw new IllegalArgumentException("onElements does not accept an empty list");
        }

        for (var attributeName : attributeNames) {
            var policy = makePolicy(attributeName);
            for (var elementName : aElementNames) {
                parent.attributePolicy(elementName, attributeName, policy);
            }
        }

        return parent;
    }

    private AttributePolicy makePolicy(QName attributeName)
    {
        if (valuePattern != null) {
            return new AttributeValueMatchingPolicy(attributeName, action, valuePattern);
        }

        return new QNameAttributePolicy(attributeName, action);
    }

    private AttributePolicy makePolicy(Pattern[] aQnamePattern)
    {
        return new QNameMatchingAttributePolicy(aQnamePattern, action, valuePattern);
    }
}
