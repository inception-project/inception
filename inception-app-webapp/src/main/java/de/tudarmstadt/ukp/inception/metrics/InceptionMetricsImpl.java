/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;

@ManagedResource
@Service
@ConditionalOnProperty(prefix = "inception.monitoring", name = "enabled", havingValue = "true")
public class InceptionMetricsImpl
    implements InceptionMetrics
{

    @Autowired
    private UserDao userRepository;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private RecommendationService recService;
    
    @Override
    @ManagedAttribute
    public long getActiveUsersTotal() {
        return sessionRegistry.getAllPrincipals().size();
    }

    @Override
    @ManagedAttribute
    public long getEnbabledUsersTotal()
    {
        return userRepository.getNumOfEnabledUsers();
    }

    @Override
    @ManagedAttribute
    public long getDocumentsTotal()
    {
        return documentService.getNumOfSourceDocuments();
    }

    @Override
    @ManagedAttribute
    public long getEnabledRecommendersTotal()
    {
        return recService.getNumOfEnabledRecommenders();
    }

    @Override
    @ManagedAttribute
    public long getAnnotationDocumentsTotal()
    {
        return documentService.getNumOfAnnotationDocuments();
    }
}
