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

import static de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils.loadPolicies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.xml.sanitizer.AttributeAction;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.ElementAction;

class PolicyCollectionIOUtilsTest
{
    @Test
    void thatEmptyPolicyFileCanBeParsed() throws Exception
    {
        var sut = loadPolicies(
                getClass().getResource("/SanitizingContentHandler/EmptyPolicy.yaml"));

        assertThat(sut.isDebug()).isFalse();

        assertThat(sut.getDefaultAttributeAction()).isEqualTo(AttributeAction.DROP);
        assertThat(sut.getDefaultElementAction()).isEqualTo(ElementAction.DROP);

        assertThat(sut.getElementPolicies()).isEmpty();

        assertThat(sut.getGlobalAttributeElementPolicies()).isEmpty();
    }

    @Test
    void thatSimplePolicyFileCanBeParsed() throws Exception
    {
        var sut = loadPolicies(getClass().getResource("/SanitizingContentHandler/Policy.yaml"));

        assertThat(sut.isDebug()).isTrue();

        assertThat(sut.getDefaultAttributeAction()).isEqualTo(AttributeAction.PASS);
        assertThat(sut.getDefaultElementAction()).isEqualTo(ElementAction.PASS);

        assertThat(sut.getElementPolicies()) //
                .extracting(p -> p.getQName().getLocalPart(), p -> p.getAction()) //
                .containsExactly( //
                        tuple("div", ElementAction.PASS), //
                        tuple("p", ElementAction.PASS), //
                        tuple("th", ElementAction.PASS), //
                        tuple("tr", ElementAction.PASS));

        assertThat(sut.getGlobalAttributeElementPolicies())
                .extracting(p -> p.getQName().getLocalPart(), p -> p.getAction()) //
                .containsExactly( //
                        tuple("style", AttributeAction.PASS));

        assertThat(sut.forAttribute(new QName("div"), new QName("title"), null, "blah")).get() //
                .isEqualTo(AttributeAction.PASS);
    }
}
