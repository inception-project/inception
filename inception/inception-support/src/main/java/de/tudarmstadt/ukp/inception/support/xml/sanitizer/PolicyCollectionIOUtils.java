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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import de.tudarmstadt.ukp.inception.support.yaml.YamlUtil;

public class PolicyCollectionIOUtils
{
    public static PolicyCollection loadPolicies(URL aUrl) throws IOException
    {
        try (var is = aUrl.openStream()) {
            return loadPolicies(is);
        }
    }

    public static PolicyCollection loadPolicies(InputStream aIs) throws IOException
    {
        var externalCollection = YamlUtil.fromYamlStream(ExternalPolicyCollection.class, aIs);

        var policyCollectionBuilder = externalCollection.isCaseSensitive()
                ? PolicyCollectionBuilder.caseSensitive()
                : PolicyCollectionBuilder.caseInsensitive();

        if (externalCollection.getDefaultElementAction() != null) {
            policyCollectionBuilder
                    .defaultElementAction(externalCollection.getDefaultElementAction());
        }
        if (externalCollection.getDefaultAttributeAction() != null) {
            policyCollectionBuilder
                    .defaultAttributeAction(externalCollection.getDefaultAttributeAction());
        }
        if (externalCollection.getDefaultNamespace() != null) {
            policyCollectionBuilder.defaultNamespace(externalCollection.getDefaultNamespace());
        }
        if (externalCollection.isMatchWithoutNamespace()) {
            policyCollectionBuilder.matchWithoutNamespace();
        }
        if (externalCollection.isUseDefaultNamespaceForAttributes()) {
            policyCollectionBuilder.useDefaultNamespaceForAttributes();
        }

        for (var policy : externalCollection.getPolicies()) {
            var isElementPolicy = policy.getElements() != null;
            var isAttributesPolicy = policy.getAttributes() != null;
            var isAttributePatternPolicy = policy.getAttributePatterns() != null;

            if (isElementPolicy && isAttributesPolicy && !isAttributePatternPolicy) {
                throw new IOException(
                        "Policy must contain either [elements] or [attributes] but not both");
            }

            if (isElementPolicy) {
                elementsPolicy(policyCollectionBuilder, policy);
            }

            if (isAttributesPolicy) {
                qnameAttributesPolicy(policyCollectionBuilder, policy);
            }

            if (isAttributePatternPolicy) {
                qnameAttributePatternsPolicy(policyCollectionBuilder, policy);
            }
        }

        policyCollectionBuilder.allowAttributes("data-capture-root").globally();

        var policies = policyCollectionBuilder.build();
        policies.setDebug(externalCollection.isDebug());

        policies.setName(externalCollection.getName());
        policies.setVersion(externalCollection.getVersion());

        return policies;

    }

    private static void qnameAttributePatternsPolicy(
            PolicyCollectionBuilder policyCollectionBuilder, ExternalPolicy policy)
    {
        var attributePatterns = policy.getAttributePatterns().stream() //
                .map(s -> Pattern.compile(s)) //
                .toArray(Pattern[]::new);
        var action = AttributeAction.valueOf(policy.getAction());
        var attrBuilder = new AttributePolicyBuilder(policyCollectionBuilder, action,
                attributePatterns);

        if (StringUtils.isNotBlank(policy.getPattern())) {
            attrBuilder.matchingValue(Pattern.compile(policy.getPattern()));
        }

        if (policy.getOnElements() != null) {
            var elements = policy.getOnElements().stream() //
                    .map(s -> QName.valueOf(s)) //
                    .toArray(QName[]::new);
            attrBuilder.onElements(elements);
        }
        else {
            attrBuilder.globally();
        }
    }

    private static void qnameAttributesPolicy(PolicyCollectionBuilder policyCollectionBuilder,
            ExternalPolicy policy)
    {
        var attributes = policy.getAttributes().stream() //
                .map(s -> QName.valueOf(s)) //
                .toArray(QName[]::new);
        var action = AttributeAction.valueOf(policy.getAction());
        var attrBuilder = new AttributePolicyBuilder(policyCollectionBuilder, action, attributes);

        if (StringUtils.isNotBlank(policy.getPattern())) {
            attrBuilder.matchingValue(Pattern.compile(policy.getPattern()));
        }

        if (policy.getOnElements() != null) {
            var elements = policy.getOnElements().stream() //
                    .map(s -> QName.valueOf(s)) //
                    .toArray(QName[]::new);
            attrBuilder.onElements(elements);
        }
        else {
            attrBuilder.globally();
        }
    }

    private static void elementsPolicy(PolicyCollectionBuilder policyCollectionBuilder,
            ExternalPolicy policy)
    {
        var elements = policy.getElements().stream() //
                .map(s -> QName.valueOf(s)) //
                .toArray(QName[]::new);
        var action = ElementAction.valueOf(policy.getAction());
        for (var element : elements) {
            policyCollectionBuilder.elementPolicy(element, action);
        }
    }
}
