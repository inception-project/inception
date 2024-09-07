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
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

class KnwoledgeBaseSchemaProfileTest
{
    @Test
    void checkKBProfileAndKBObject_ShouldReturnMatchingSchemaProfile()
    {
        var name = "Test KB";
        var classIri = "http://www.w3.org/2002/07/owl#Class";
        var subclassIri = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        var typeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        var subPropertyIri = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        var label = "http://www.w3.org/2000/01/rdf-schema#label";
        var propertyTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        var descriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        var propertyLabelIri = "http://www.w3.org/2000/01/rdf-schema#label";
        var propertyDescriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        var deprecationPropertyIri = "http://www.w3.org/2002/07/owl#deprecated";

        var testMapping = new KnowledgeBaseMapping(classIri, subclassIri, typeIri, subPropertyIri,
                descriptionIri, label, propertyTypeIri, propertyLabelIri, propertyDescriptionIri,
                deprecationPropertyIri);
        var testProfile = new KnowledgeBaseProfile();
        testProfile.setName(name);
        testProfile.setMapping(testMapping);

        var testKb = new KnowledgeBase();
        testKb.applyMapping(testMapping);

        assertThat(SchemaProfile.checkSchemaProfile(testProfile))
                .isEqualTo(SchemaProfile.OWLSCHEMA);
        assertThat(SchemaProfile.checkSchemaProfile(testKb)).isEqualTo(SchemaProfile.OWLSCHEMA);
    }
}
