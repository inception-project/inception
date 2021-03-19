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
package de.tudarmstadt.ukp.inception.kb.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class KBHandleTest
{
    @Test
    public void thatUILabelExtractsGoodLabelFromIRI()
    {
        assertThat(new KBHandle("http://dummy.org/ontology#"))
                .as("IRI has anchor symbol but nothing after it").extracting(KBHandle::getUiLabel)
                .isEqualTo("http://dummy.org/ontology#");

        assertThat(new KBHandle("http://dummy.org/ontology")).as("IRI ends in a path segment")
                .extracting(KBHandle::getUiLabel).isEqualTo("ontology");

        assertThat(new KBHandle("http://dummy.org/ontology#someConcept")).as("IRI has named anchor")
                .extracting(KBHandle::getUiLabel).isEqualTo("someConcept");

        assertThat(new KBHandle("http://dummy.org/ontology#some_Concept"))
                .as("IRI has named anchor in snake_case").extracting(KBHandle::getUiLabel)
                .isEqualTo("some Concept");
    }
}
