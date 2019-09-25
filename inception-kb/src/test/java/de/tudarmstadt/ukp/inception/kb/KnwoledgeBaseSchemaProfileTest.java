/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public class KnwoledgeBaseSchemaProfileTest
{
    @Test public void checkKBProfileAndKBObject_ShouldReturnMatchingSchemaProfile()
    {
        String name = "Test KB";
        String classIri = "http://www.w3.org/2002/07/owl#Class";
        String subclassIri = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        String typeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String subPropertyIri = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        String label = "http://www.w3.org/2000/01/rdf-schema#label";
        String propertyTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        String descriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        String propertyLabelIri = "http://www.w3.org/2000/01/rdf-schema#label";
        String propertyDescriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";

        KnowledgeBaseMapping testMapping = new KnowledgeBaseMapping(classIri, subclassIri, typeIri, subPropertyIri,
            descriptionIri, label, propertyTypeIri, propertyLabelIri, propertyDescriptionIri);
        KnowledgeBaseProfile testProfile = new KnowledgeBaseProfile();
        testProfile.setName(name);
        testProfile.setMapping(testMapping);

        KnowledgeBase testKb = new KnowledgeBase();
        testKb.applyMapping(testMapping);

        assertThat(SchemaProfile.checkSchemaProfile(testProfile))
            .isEqualTo(SchemaProfile.OWLSCHEMA);
        assertThat(SchemaProfile.checkSchemaProfile(testKb))
            .isEqualTo(SchemaProfile.OWLSCHEMA);
    }
}
