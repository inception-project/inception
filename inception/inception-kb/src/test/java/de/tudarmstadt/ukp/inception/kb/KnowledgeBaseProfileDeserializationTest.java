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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseAccess;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public class KnowledgeBaseProfileDeserializationTest
{
    private final String KNOWLEDGEBASE_TEST_PROFILES_YAML = "kb_test_profiles.yaml";

    @Test
    public void checkThatDeserializationWorks() throws IOException
    {
        var name = "Test KB";
        var rootConcepts = new ArrayList<String>();
        var type = RepositoryType.LOCAL;
        var reification = Reification.WIKIDATA;
        var defaultLanguage = "en";

        var referenceProfile = new KnowledgeBaseProfile();

        referenceProfile.setName(name);
        referenceProfile.setType(type);
        referenceProfile.setRootConcepts(rootConcepts);
        referenceProfile.setDefaultLanguage(defaultLanguage);
        referenceProfile.setReification(reification);
        referenceProfile.setMapping(createReferenceMapping());
        referenceProfile.setAccess(createReferenceAccess());
        referenceProfile.setInfo(createReferenceInfo());

        var resolver = new PathMatchingResourcePatternResolver();
        Map<String, KnowledgeBaseProfile> profiles;
        try (var r = new InputStreamReader(
                resolver.getResource(KNOWLEDGEBASE_TEST_PROFILES_YAML).getInputStream())) {
            var mapper = new ObjectMapper(new YAMLFactory());
            profiles = mapper.readValue(r,
                    new TypeReference<HashMap<String, KnowledgeBaseProfile>>()
                    {
                    });
        }
        var testProfile = profiles.get("test_profile");
        assertThat(testProfile).usingRecursiveComparison().isEqualTo(referenceProfile);
    }

    private KnowledgeBaseInfo createReferenceInfo()
    {
        var referenceInfo = new KnowledgeBaseInfo();
        referenceInfo.setDescription("This is a knowledge base for testing the kb profiles");
        referenceInfo.setAuthorName("INCEpTION team");
        referenceInfo.setHostInstitutionName("a host");
        referenceInfo.setWebsiteUrl("https://inception-project.github.io/");
        return referenceInfo;
    }

    private KnowledgeBaseMapping createReferenceMapping()
    {
        var classIri = "http://www.w3.org/2000/01/rdf-schema#Class";
        var subclassIri = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        var typeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        var subPropertyIri = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        var label = "http://www.w3.org/2000/01/rdf-schema#label";
        var propertyTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        var descriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        var propertyLabelIri = "http://www.w3.org/2000/01/rdf-schema#label";
        var propertyDescriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        var deprecationPropertyIri = "http://www.w3.org/2002/07/owl#deprecated";
        var referenceMapping = new KnowledgeBaseMapping(classIri, subclassIri, typeIri,
                subPropertyIri, descriptionIri, label, propertyTypeIri, propertyLabelIri,
                propertyDescriptionIri, deprecationPropertyIri);
        return referenceMapping;
    }

    private KnowledgeBaseAccess createReferenceAccess()
    {
        var url = "http://someurl/sparql";
        var fullTextSearchIri = "http://www.openrdf.org/contrib/lucenesail#matches";
        return new KnowledgeBaseAccess(url, fullTextSearchIri);
    }
}
