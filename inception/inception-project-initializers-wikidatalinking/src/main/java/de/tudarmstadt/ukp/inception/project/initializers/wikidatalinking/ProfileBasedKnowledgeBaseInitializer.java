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

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.KnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;

public class ProfileBasedKnowledgeBaseInitializer
    implements KnowledgeBaseInitializer
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CLASSPATH_PREFIX = "classpath:";

    private final PackageResourceReference thumbnail;
    private final KnowledgeBaseService kbService;
    private final KnowledgeBaseProfile profile;

    public ProfileBasedKnowledgeBaseInitializer(KnowledgeBaseService aKbService,
            KnowledgeBaseProfile aProfile, PackageResourceReference aThumbnail)
    {
        kbService = aKbService;
        thumbnail = aThumbnail;
        profile = aProfile;
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
        return Optional.of(thumbnail);
    }

    @Override
    public Optional<String> getAuthorName()
    {
        return Optional.ofNullable(profile.getInfo().getAuthorName());
    }

    @Override
    public Optional<String> getHostInstitutionName()
    {
        return Optional.ofNullable(profile.getInfo().getHostInstitutionName());
    }

    @Override
    public Optional<String> getLicenseName()
    {
        return Optional.ofNullable(profile.getInfo().getLicenseName());
    }

    @Override
    public Optional<String> getLicenseUrl()
    {
        return Optional.ofNullable(profile.getInfo().getLicenseUrl());
    }

    @Override
    public Optional<String> getWebsiteUrl()
    {
        return Optional.ofNullable(profile.getInfo().getWebsiteUrl());
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
        kb.setReadOnly(profile.getType() == REMOTE);

        RepositoryImplConfig cfg;
        if (profile.getType() == REMOTE) {
            cfg = kbService.getRemoteConfig(profile.getAccess().getAccessUrl());
        }
        else {
            cfg = kbService.getNativeConfig();
        }

        kbService.registerKnowledgeBase(kb, cfg);

        var accessUrl = profile.getAccess().getAccessUrl();
        if (accessUrl == null) {
            // Nothing to do
        }
        else if (accessUrl.startsWith(CLASSPATH_PREFIX)) {
            var fileName = substringAfterLast(accessUrl, "/");
            var resolver = new PathMatchingResourcePatternResolver();
            try (var is = resolver.getResource(accessUrl).getInputStream()) {
                kbService.importData(kb, fileName, is);
            }
        }
        else {
            var pathName = Paths.get(accessUrl);
            var fileName = pathName.getFileName().toString();
            try (var is = new URL(accessUrl).openStream()) {
                kbService.importData(kb, fileName, is);
            }
        }
    }
}
