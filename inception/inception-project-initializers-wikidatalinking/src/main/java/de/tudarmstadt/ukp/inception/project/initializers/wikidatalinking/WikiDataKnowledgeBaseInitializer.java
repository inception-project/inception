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
package de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config.WikiDataLinkingProjectInitializersAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WikiDataLinkingProjectInitializersAutoConfiguration#wikiDataKnowledgeBaseInitializer}.
 * </p>
 */
public class WikiDataKnowledgeBaseInitializer
    implements ProjectInitializer
{
    private static final String WIKIDATA_PROFILE = "wikidata";

    private final KnowledgeBaseService kbService;
    private final KnowledgeBaseProfile wikidataProfile;
    private final KnowledgeBaseProperties kbProperties;

    public WikiDataKnowledgeBaseInitializer(KnowledgeBaseService aKbService,
            KnowledgeBaseProperties aKbProperties)
    {
        kbService = aKbService;
        kbProperties = aKbProperties;

        try {
            wikidataProfile = KnowledgeBaseProfile.readKnowledgeBaseProfiles()
                    .get(WIKIDATA_PROFILE);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName()
    {
        return "Wikidata knowledge base";
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return kbService.knowledgeBaseExists(aProject, wikidataProfile.getName());
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return emptyList();
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        // set all the fields according to the chosen profile
        RepositoryImplConfig cfg = kbService
                .getRemoteConfig(wikidataProfile.getAccess().getAccessUrl());

        // sets root concepts list - if null then an empty list otherwise change the
        // values to IRI and populate the list
        KnowledgeBase kb = new KnowledgeBase();
        kb.setProject(aProject);
        kb.setReadOnly(true);
        kb.setMaxResults(kbProperties.getDefaultMaxResults());
        kb.setType(wikidataProfile.getType());
        kb.setName(wikidataProfile.getName());
        kb.applyRootConcepts(wikidataProfile);
        kb.applyMapping(wikidataProfile.getMapping());
        kb.setFullTextSearchIri(wikidataProfile.getAccess().getFullTextSearchIri());
        kb.setDefaultLanguage(wikidataProfile.getDefaultLanguage());
        kb.setDefaultDatasetIri(wikidataProfile.getDefaultDataset());
        kb.setReification(wikidataProfile.getReification());

        kbService.registerKnowledgeBase(kb, cfg);
    }
}
