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

import static de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils.caseInsensitiveQNameComparator;
import static de.tudarmstadt.ukp.inception.support.xml.sanitizer.ElementAction.PASS;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyCollectionBuilder
{
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("rawtypes")
    private final Supplier<? extends Map> mapSupplier;
    private final Map<QName, ElementPolicyBuilder> elementPolicyBuilders;
    private final Map<QName, Map<QName, AttributePolicy>> elementAttributePolicies;
    private final Map<QName, AttributePolicy> globalAttributePolicies;

    private ElementAction defaultElementAction = ElementAction.DROP;
    private AttributeAction defaultAttributeAction = AttributeAction.DROP;

    private String defaultNamespace;
    private boolean useDefaultNamespaceForAttributes = true;
    private boolean matchWithoutNamespace = false;

    public static PolicyCollectionBuilder caseSensitive()
    {
        return new PolicyCollectionBuilder(LinkedHashMap::new);
    }

    public static PolicyCollectionBuilder caseInsensitive()
    {
        return new PolicyCollectionBuilder(
                () -> new TreeMap<QName, String>(caseInsensitiveQNameComparator()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PolicyCollectionBuilder(Supplier<? extends Map> aMapSupplier)
    {
        mapSupplier = aMapSupplier;
        elementPolicyBuilders = mapSupplier.get();
        elementAttributePolicies = mapSupplier.get();
        globalAttributePolicies = mapSupplier.get();
    }

    public PolicyCollectionBuilder defaultNamespace(String aDefaultNamespace)
    {
        defaultNamespace = aDefaultNamespace;
        return this;
    }

    public PolicyCollectionBuilder useDefaultNamespaceForAttributes()
    {
        useDefaultNamespaceForAttributes = true;
        return this;
    }

    public PolicyCollectionBuilder matchWithoutNamespace()
    {
        matchWithoutNamespace = true;
        return this;
    }

    public PolicyCollectionBuilder defaultAttributeAction(AttributeAction aDefaultAttributeAction)
    {
        defaultAttributeAction = aDefaultAttributeAction;
        return this;
    }

    public PolicyCollectionBuilder defaultElementAction(ElementAction aDefaultElementAction)
    {
        defaultElementAction = aDefaultElementAction;
        return this;
    }

    public PolicyCollectionBuilder allowElements(String... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(new QName(elementName), PASS);
        }

        return this;
    }

    public PolicyCollectionBuilder allowElements(QName... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(elementName, PASS);
        }

        return this;
    }

    public PolicyCollectionBuilder disallowElements(String... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(new QName(elementName), ElementAction.DROP);
        }

        return this;
    }

    public PolicyCollectionBuilder disallowElements(QName... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(elementName, ElementAction.DROP);
        }

        return this;
    }

    public PolicyCollectionBuilder pruneElements(String... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(new QName(elementName), ElementAction.PRUNE);
        }

        return this;
    }

    public PolicyCollectionBuilder pruneElements(QName... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(elementName, ElementAction.PRUNE);
        }

        return this;
    }

    public PolicyCollectionBuilder skipElements(String... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(new QName(elementName), ElementAction.SKIP);
        }

        return this;
    }

    public PolicyCollectionBuilder skipElements(QName... aElementNames)
    {
        for (var elementName : aElementNames) {
            elementPolicy(elementName, ElementAction.SKIP);
        }

        return this;
    }

    PolicyCollectionBuilder elementPolicy(QName aElement, ElementAction aAction)
    {
        if (defaultNamespace == null) {
            _elementPolicy(aElement, aAction);
            return this;
        }

        if (isEmpty(aElement.getNamespaceURI())) {
            _elementPolicy(new QName(defaultNamespace, aElement.getLocalPart()), aAction);

            if (matchWithoutNamespace) {
                _elementPolicy(aElement, aAction);
            }
        }

        return this;
    }

    private void _elementPolicy(QName aElement, ElementAction aAction)
    {
        elementPolicyBuilders.put(aElement,
                new ElementPolicyBuilder(aElement, aAction, mapSupplier));
    }

    public AttributePolicyBuilder allowAttributes(String... aAttributeNames)
    {
        var attributeNames = Stream.of(aAttributeNames).map(QName::new).toArray(QName[]::new);

        return new AttributePolicyBuilder(this, AttributeAction.PASS, attributeNames);
    }

    public AttributePolicyBuilder allowAttributes(QName... aAttributeNames)
    {
        return new AttributePolicyBuilder(this, AttributeAction.PASS, aAttributeNames);
    }

    public AttributePolicyBuilder disallowAttributes(String... aAttributeNames)
    {
        var attributeNames = Stream.of(aAttributeNames).map(QName::new).toArray(QName[]::new);
        return new AttributePolicyBuilder(this, AttributeAction.DROP, attributeNames);
    }

    public AttributePolicyBuilder disallowAttributes(QName... aAttributeNames)
    {
        return new AttributePolicyBuilder(this, AttributeAction.DROP, aAttributeNames);
    }

    public PolicyCollection build()
    {
        @SuppressWarnings("unchecked")
        Map<QName, ElementPolicy> elementPolicies = mapSupplier.get();

        for (var entry : elementAttributePolicies.entrySet()) {
            var elementPolicyBuilder = elementPolicyBuilders.computeIfAbsent(entry.getKey(),
                    k -> new ElementPolicyBuilder(k, defaultElementAction, mapSupplier));
            elementPolicyBuilder.attributePolicies(elementAttributePolicies.get(entry.getKey()));
        }

        for (var entry : elementPolicyBuilders.entrySet()) {
            elementPolicies.put(entry.getKey(), entry.getValue().build());
        }

        return new PolicyCollection(defaultNamespace, elementPolicies, globalAttributePolicies,
                defaultElementAction, defaultAttributeAction);
    }

    void attributePolicy(QName aElementName, QName aAttributeName, AttributePolicy aPolicy)
    {
        if (defaultNamespace == null) {
            _attributePolicy(aElementName, aAttributeName, aPolicy);
            return;
        }

        if (isEmpty(aElementName.getNamespaceURI()) && isEmpty(aAttributeName.getNamespaceURI())) {
            var elementName = new QName(defaultNamespace, aElementName.getLocalPart());
            var attributeName = useDefaultNamespaceForAttributes
                    ? new QName(defaultNamespace, aAttributeName.getLocalPart())
                    : aAttributeName;
            _attributePolicy(elementName, attributeName, aPolicy);

            if (matchWithoutNamespace) {
                _attributePolicy(aElementName, aAttributeName, aPolicy);
            }
        }
    }

    private void _attributePolicy(QName aElementName, QName aAttributeName, AttributePolicy aPolicy)
    {
        @SuppressWarnings("unchecked")
        Map<QName, AttributePolicy> attributePolicies = elementAttributePolicies
                .computeIfAbsent(aElementName, k -> mapSupplier.get());
        var attributePolicy = attributePolicies.computeIfAbsent(aAttributeName,
                k -> AttributePolicy.UNDEFINED);

        if (aPolicy instanceof DelegatingAttributePolicy) {
            ((DelegatingAttributePolicy) aPolicy).setDelegate(attributePolicy);
            attributePolicies.put(aAttributeName, aPolicy);
            return;
        }

        var oldPolicy = attributePolicies.put(aAttributeName, aPolicy);
        if (!AttributePolicy.isUndefined(oldPolicy)) {
            log.warn("On element [{}] overriding policy for attribute [{}]: [{}] -> [{}]",
                    aElementName, aAttributeName, oldPolicy, aPolicy);
        }
    }

    public void allowAttribute(QName aAttribute, Pattern aPattern)
    {
        globalAttributePolicy(new AttributePolicy(aAttribute, AttributeAction.PASS));
    }

    public void disallowAttribute(QName aAttribute, Pattern aPattern)
    {
        globalAttributePolicy(new AttributePolicy(aAttribute, AttributeAction.PASS));
    }

    public void globalAttributePolicy(AttributePolicy aPolicy)
    {
        if (defaultNamespace == null) {
            _globalAttributePolicy(aPolicy);
            return;
        }

        if (isEmpty(aPolicy.getQName().getNamespaceURI())) {
            var attributeName = useDefaultNamespaceForAttributes
                    ? new QName(defaultNamespace, aPolicy.getQName().getLocalPart())
                    : aPolicy.getQName();
            _globalAttributePolicy(new AttributePolicy(attributeName, aPolicy.getAction()));

            if (matchWithoutNamespace) {
                _globalAttributePolicy(aPolicy);
            }
        }
    }

    private void _globalAttributePolicy(AttributePolicy aPolicy)
    {
        var newPolicy = aPolicy;
        var oldPolicy = globalAttributePolicies.put(aPolicy.getQName(), newPolicy);
        if (!AttributePolicy.isUndefined(oldPolicy)
                && oldPolicy.getAction() != newPolicy.getAction()) {
            log.warn("Globally overriding policy for attribute [{}]: [{}] -> [{}]",
                    aPolicy.getQName(), oldPolicy, newPolicy);
        }
    }

    // XmlPolicyBuilder allowWithoutAttributes(String... aElementNames);
    // XmlPolicyBuilder disallowWithoutAttributes(String... aElementNames);
    // XmlPolicyBuilder allowTextIn(String... aElementNames);
    // XmlPolicyBuilder disallowTextIn(String... aElementNames);
    // XmlPolicyBuilder allowAttributesGlobally(String... aAttributeNames);
    // XmlPolicyBuilder allowAttributesOnElements(String... aElementNames);
}
