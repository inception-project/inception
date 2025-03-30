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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.KnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config.WikiDataLinkingProjectInitializersAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WikiDataLinkingProjectInitializersAutoConfiguration#wikiDataKnowledgeBaseInitializer}.
 * </p>
 */
public class WikiDataKnowledgeBaseInitializer
    implements KnowledgeBaseInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "Lexicon.svg");

    private final KnowledgeBaseService kbService;
    private final KnowledgeBaseProfile profile;

    public WikiDataKnowledgeBaseInitializer(KnowledgeBaseService aKbService)
    {
        kbService = aKbService;

        try {
            profile = KnowledgeBaseProfile.readKnowledgeBaseProfiles().get("wikidata");
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName()
    {
        return profile.getName();
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.ofNullable(profile.getInfo().getDescription());
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return kbService.knowledgeBaseExists(aProject, profile.getName());
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var kb = new KnowledgeBase();
        kbService.configure(kb, profile);
        kb.setProject(aRequest.getProject());
        kb.setReadOnly(true);

        var cfg = kbService.getRemoteConfig(profile.getAccess().getAccessUrl());
        kbService.registerKnowledgeBase(kb, cfg);
    }
}
