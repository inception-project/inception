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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PolicyCollectionTest
{
    static List<Arguments> builderVariants()
    {
        return asList( //
                Arguments.of("CASE-SENSITIVE", PolicyCollectionBuilder.caseSensitive()), //
                Arguments.of("CASE-INSENSITIVE", PolicyCollectionBuilder.caseInsensitive()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDefaultElementPolicyIsToDrop(String aName, PolicyCollectionBuilder aBuilder)
    {
        var sut = aBuilder.build();

        assertThat(sut.forElement(new QName("body"))).isEmpty();
        assertThat(sut.getDefaultElementAction()).isEqualTo(ElementAction.DROP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatPassPolicyForElementIsAccepted(String aName, PolicyCollectionBuilder aBuilder)
    {
        var goodElement = new QName("good");

        var policies = aBuilder //
                .allowElements(goodElement) //
                .build();

        assertThat(policies.forElement(goodElement)).get() //
                .extracting(QNameElementPolicy::getAction) //
                .isEqualTo(ElementAction.PASS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDropPolicyForElementIsAccepted(String aName, PolicyCollectionBuilder aBuilder)
    {
        var goodElement = new QName("good");

        var policies = aBuilder //
                .disallowElements(goodElement) //
                .build();

        assertThat(policies.forElement(goodElement)).get() //
                .extracting(QNameElementPolicy::getAction) //
                .isEqualTo(ElementAction.DROP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDropPolicyForAttributeOnElementIsAccepted(String aName,
            PolicyCollectionBuilder aBuilder)
    {
        var element = new QName("element");
        var attr = new QName("attr");

        var policies = aBuilder //
                .disallowAttributes(attr).onElements(element) //
                .build();

        assertThat(policies.forAttribute(element, attr, null, null)).get() //
                .isEqualTo(AttributeAction.DROP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDropPolicyForAttributeGloballyIsAccepted(String aName,
            PolicyCollectionBuilder aBuilder)
    {
        var element = new QName("element");
        var attr = new QName("attr");

        var policies = aBuilder //
                .disallowAttributes(attr).globally() //
                .build();

        assertThat(policies.forAttribute(element, attr, null, null)).get() //
                .isEqualTo(AttributeAction.DROP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatDefaultElementAttributePolicyIsToDrop(String aName, PolicyCollectionBuilder aBuilder)
    {
        var sut = aBuilder.build();

        assertThat(sut.forAttribute(new QName("body"), new QName("style"), null, null)) //
                .isEmpty();
        assertThat(sut.getDefaultAttributeAction()) //
                .isEqualTo(AttributeAction.DROP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatPassPolicyForElementAttributeIsAccepted(String aName, PolicyCollectionBuilder aBuilder)
    {
        var goodElement = new QName("good");
        var goodAttribute = new QName("gattr");
        var badAttribute = new QName("battr");

        var policies = aBuilder //
                .allowElements(goodElement) //
                .allowAttributes(goodAttribute).onElements(goodElement) //
                .build();

        assertThat(policies.forAttribute(goodElement, goodAttribute, null, null)).get() //
                .isEqualTo(AttributeAction.PASS);

        assertThat(policies.forAttribute(goodElement, badAttribute, null, null)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatLocalAttributePolicyTakesPrecedenceOverGlobalAttributePolicy(String aName,
            PolicyCollectionBuilder aBuilder)
    {
        var element = new QName("element");
        var otherElement = new QName("otherElement");
        var attribute = new QName("attr");

        var policies = aBuilder //
                .allowElements(element) //
                .allowAttributes(attribute).globally() //
                .disallowAttributes(attribute).onElements(element) //
                .build();

        assertThat(policies.forAttribute(element, attribute, null, null)).get() //
                .isEqualTo(AttributeAction.DROP);

        assertThat(policies.forAttribute(otherElement, attribute, null, null)).get() //
                .isEqualTo(AttributeAction.PASS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builderVariants")
    void thatPassPolicyForGlobalAttributeNameMatchingIsAccepted(String aName,
            PolicyCollectionBuilder aBuilder)
    {
        var element = new QName("element");
        var otherElement = new QName("otherElement");
        var attribute = new QName("data-forbidden");
        var attributeNamePattern = Pattern.compile("data-.*");

        var policies = aBuilder //
                .allowElements(element) //
                .allowAttributes(attributeNamePattern).globally() //
                .disallowAttributes(attribute).onElements(element) //
                .build();

        assertThat(policies.forAttribute(element, attribute, null, null)).get() //
                .isEqualTo(AttributeAction.DROP);

        assertThat(policies.forAttribute(otherElement, attribute, null, null)).get() //
                .isEqualTo(AttributeAction.PASS);
    }
}
