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
package de.tudarmstadt.ukp.inception.preferences.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.preferences.ClientSiderUserPreferencesProviderRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesServiceImpl;
import de.tudarmstadt.ukp.inception.preferences.exporter.DefaultProjectPreferencesExporter;
import de.tudarmstadt.ukp.inception.preferences.exporter.UserProjectPreferencesExporter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class PreferencesServiceAutoConfig
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public PreferencesService preferencesService()
    {
        return new PreferencesServiceImpl(entityManager);
    }

    @Bean
    public DefaultProjectPreferencesExporter defaultPreferenceExporter(
            PreferencesService aPreferencesService)
    {
        return new DefaultProjectPreferencesExporter(aPreferencesService);
    }

    @Bean
    public UserProjectPreferencesExporter userPreferencesExporter(
            PreferencesService aPreferencesService, UserDao aUserRepository)
    {
        return new UserProjectPreferencesExporter(aPreferencesService, aUserRepository);
    }

    @Bean
    public ClientSiderUserPreferencesProviderRegistry clientSiderUserPreferencesProviderRegistry(
            @Lazy @Autowired(required = false) List<ClientSideUserPreferencesProvider> aExtensions)
    {
        return new ClientSiderUserPreferencesProviderRegistry(aExtensions);
    }
}
