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
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.preferences.model.DefaultProjectPreference;
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
    private static final Logger LOG = LoggerFactory.getLogger(PreferencesServiceImpl.class);

    private final @PersistenceContext EntityManager entityManager;

    public PreferencesServiceImpl(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public <T> T loadTraitsForUser(Key<T> aKey, User aUser)
    {
        try {
            Optional<UserPreference> preference = getRawUserPreference(aKey, aUser);
            if (preference.isPresent()) {
                String json = preference.get().getTraits();
                T result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
                LOG.debug("Loaded preferences for key {} and user {}: [{}]", aKey, aUser, result);
                return result;
            }
            else {
                LOG.debug("No preferences found for key {} and user {}", aKey, aUser);
                return buildDefault(aKey.getTraitClass());
            }
        }
        catch (IOException e) {
            LOG.error("Error while loading traits, returning default", e);
            return buildDefault(aKey.getTraitClass());
        }
    }

    @Override
    @Transactional
    public <T> void saveTraitsForUser(Key<T> aKey, User aUser, T aTraits)
    {
        try {
            UserPreference preference = getRawUserPreference(aKey, aUser)
                    .orElseGet(UserPreference::new);
            preference.setUser(aUser);
            preference.setName(aKey.getName());
            preference.setTraits(toJsonString(aTraits));
            entityManager.persist(preference);

            LOG.debug("Saved preferences for key {} and user {}: [{}]", aKey, aUser, aTraits);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }

    private <T> Optional<UserPreference> getRawUserPreference(Key<T> aKey, User aUser)
    {
        String query = String.join("\n", //
                "FROM UserPreference ", //
                "WHERE user = :user ", //
                "AND name = :name");

        String name = aKey.getName();
        try {
            UserPreference pref = entityManager.createQuery(query, UserPreference.class) //
                    .setParameter("user", aUser) //
                    .setParameter("name", name) //
                    .getSingleResult();
            return Optional.of(pref);
        }
        catch (NoResultException e) {
            LOG.debug("No preferences found for key {} and user {}", name, aUser, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public <T> T loadTraitsForUserAndProject(Key<T> aKey, User aUser, Project aProject)
    {
        try {
            Optional<UserProjectPreference> pref = getUserProjectPreference(aKey, aUser, aProject);
            if (pref.isPresent()) {
                String json = pref.get().getTraits();
                T result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
                LOG.debug("Loaded preferences for key {} and user {} and project {}: [{}]", aKey,
                        aUser, aProject, result);
                return result;
            }
            else {
                return loadDefaultTraitsForProject(aKey, aProject);
            }
        }
        catch (IOException e) {
            LOG.error("Error while loading traits, returning default", e);
            return buildDefault(aKey.getTraitClass());
        }
    }

    @Override
    @Transactional
    public <T> void saveTraitsForUserAndProject(Key<T> aKey, User aUser, Project aProject,
            T aTraits)
    {
        try {
            UserProjectPreference preference = getUserProjectPreference(aKey, aUser, aProject)
                    .orElseGet(UserProjectPreference::new);
            preference.setUser(aUser);
            preference.setProject(aProject);
            preference.setName(aKey.getName());
            preference.setTraits(toJsonString(aTraits));
            entityManager.persist(preference);

            LOG.debug("Saved preferences for key {} and user {} and project {}: [{}]", aKey, aUser,
                    aProject, aTraits);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }

    private <T> Optional<UserProjectPreference> getUserProjectPreference(Key<T> aKey, User aUser,
            Project aProject)
    {
        String query = String.join("\n", //
                "FROM UserProjectPreference ", //
                "WHERE user = :user", //
                "AND project = :project", //
                "AND name = :name");

        try {
            UserProjectPreference pref = entityManager //
                    .createQuery(query, UserProjectPreference.class) //
                    .setParameter("user", aUser) //
                    .setParameter("project", aProject) //
                    .setParameter("name", aKey.getName()) //
                    .getSingleResult();

            return Optional.of(pref);
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public List<UserProjectPreference> listUserPreferencesForProject(Project aProject)
    {
        String query = String.join("\n", //
                "FROM UserProjectPreference ", //
                "WHERE project = :project");

        return entityManager //
                .createQuery(query, UserProjectPreference.class) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public void saveUserProjectPreference(UserProjectPreference aPreference)
    {
        entityManager.persist(aPreference);
    }

    @Override
    @Transactional
    public <T> T loadDefaultTraitsForProject(Key<T> aKey, Project aProject)
    {
        try {
            Optional<DefaultProjectPreference> pref = getDefaultProjectPreference(aKey, aProject);
            if (pref.isPresent()) {
                String json = pref.get().getTraits();
                T result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
                LOG.debug("Loaded default preferences for key {} and project {}: [{}]", aKey,
                        aProject, result);
                return result;
            }
            else {
                LOG.trace("No default preferences found for key {} and project {}", aKey, aProject);
                return buildDefault(aKey.getTraitClass());
            }
        }
        catch (IOException e) {
            LOG.error("Error while loading traits, returning default", e);
            return buildDefault(aKey.getTraitClass());
        }
    }

    @Override
    @Transactional
    public <T> void saveDefaultTraitsForProject(Key<T> aKey, Project aProject, T aTraits)
    {
        try {
            DefaultProjectPreference preference = getDefaultProjectPreference(aKey, aProject)
                    .orElseGet(DefaultProjectPreference::new);
            preference.setProject(aProject);
            preference.setName(aKey.getName());
            preference.setTraits(toJsonString(aTraits));
            entityManager.persist(preference);

            LOG.debug("Saved default preferences for key {} and project {}: [{}]", aKey, aProject,
                    aTraits);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }

    @Override
    public <T> void clearDefaultTraitsForProject(Key<T> aKey, Project aProject)
    {
        String query = String.join("\n", //
                "DELETE DefaultProjectPreference ", //
                "WHERE project = :project", //
                "AND name = :name");

        entityManager.createQuery(query) //
                .setParameter("project", aProject) //
                .setParameter("name", aKey.getName()) //
                .executeUpdate();
    }

    private <T> Optional<DefaultProjectPreference> getDefaultProjectPreference(Key<T> aKey,
            Project aProject)
    {
        String query = String.join("\n", //
                "FROM DefaultProjectPreference ", //
                "WHERE project = :project", //
                "AND name = :name");

        try {
            DefaultProjectPreference pref = entityManager //
                    .createQuery(query, DefaultProjectPreference.class) //
                    .setParameter("project", aProject) //
                    .setParameter("name", aKey.getName()) //
                    .getSingleResult();

            return Optional.of(pref);
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public List<DefaultProjectPreference> listDefaultTraitsForProject(Project aProject)
    {
        String query = String.join("\n", //
                "FROM DefaultProjectPreference ", //
                "WHERE project = :project");

        return entityManager //
                .createQuery(query, DefaultProjectPreference.class) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public void saveDefaultProjectPreference(DefaultProjectPreference aPreference)
    {
        entityManager.persist(aPreference);
    }

    /*
     * Use default constructor of aClass to create new instance of T
     */
    private <T> T buildDefault(Class<T> aClass)
    {
        try {
            return aClass.getConstructor().newInstance();
        }
        catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}
