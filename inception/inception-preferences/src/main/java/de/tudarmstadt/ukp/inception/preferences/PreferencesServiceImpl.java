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
package de.tudarmstadt.ukp.inception.preferences;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.preferences.model.UserPreference;
import de.tudarmstadt.ukp.inception.preferences.model.UserProjectPreference;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PreferencesServiceAutoConfig#preferencesService}.
 * </p>
 */
public class PreferencesServiceImpl
    implements PreferencesService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesServiceImpl.class);

    private @PersistenceContext EntityManager entityManager;

    public PreferencesServiceImpl(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public <T> Optional<T> loadTraitsForUser(Key<T> aKey, User aUser)
    {
        String query = String.join("\n", //
                "SELECT traits", "FROM UserPreference ", //
                "WHERE user = :user ", //
                "AND name = :name");

        List<String> traits = entityManager.createQuery(query, String.class) //
                .setParameter("user", aUser) //
                .setParameter("name", aKey.getName()) //
                .getResultList();

        if (traits.isEmpty()) {
            return Optional.empty();
        }
        
        if (traits.size() > 1) {
            throw new IllegalStateException("Found more than one preference [" + aKey.getName()
                    + "] for user [" + aUser.getUsername() + "]");
        }
        
        try {
            return Optional.of(JSONUtil.fromJsonString(aKey.getTraitClass(), traits.get(0)));
        }
        catch (IOException e) {
            LOGGER.error("Error while loading traits", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public <T> void saveTraitsForUser(Key<T> aKey, User aUser, T aTraits)
    {
        try {
            UserPreference preference = new UserPreference();
            preference.setUser(aUser);
            preference.setName(aKey.getName());
            preference.setTraits(toJsonString(aTraits));
            entityManager.persist(preference);
        }
        catch (IOException e) {
            LOGGER.error("Error while writing traits", e);
        }
    }

    @Override
    @Transactional
    public <T> Optional<T> loadTraitsForUserAndProject(Key<T> aKey, User aUser, Project aProject)
    {
        String query = String.join("\n", //
                "SELECT traits", "FROM UserProjectPreference ", //
                "WHERE user = :user ", //
                "AND project = :project", //
                "AND name = :name");

        String traits = entityManager.createQuery(query, String.class) //
                .setParameter("user", aUser) //
                .setParameter("project", aProject) //
                .setParameter("name", aKey.getName()) //
                .getSingleResult();

        try {
            return Optional.of(JSONUtil.fromJsonString(aKey.getTraitClass(), traits));
        }
        catch (IOException e) {
            LOGGER.error("Error while loading traits", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public <T> void saveTraitsForUserAndProject(Key<T> aKey, User aUser, Project aProject,
            T aTraits)
    {
        try {
            UserProjectPreference preference = new UserProjectPreference();
            preference.setUser(aUser);
            preference.setProject(aProject);
            preference.setName(aKey.getName());
            preference.setTraits(toJsonString(aTraits));
            entityManager.persist(preference);
        }
        catch (IOException e) {
            LOGGER.error("Error while writing traits", e);
        }
    }
}
