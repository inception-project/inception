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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.preferences.model.DefaultProjectPreference;
import de.tudarmstadt.ukp.inception.preferences.model.UserPreference;
import de.tudarmstadt.ukp.inception.preferences.model.UserProjectPreference;
import de.tudarmstadt.ukp.inception.preferences.model.UserProjectPreference_;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PreferencesServiceAutoConfig#preferencesService}.
 * </p>
 */
public class PreferencesServiceImpl
    implements PreferencesService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final @PersistenceContext EntityManager entityManager;

    public PreferencesServiceImpl(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public <T extends PreferenceValue> T loadTraitsForUser(PreferenceKey<T> aKey, User aUser)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");

        try {
            var preference = getRawUserPreference(aKey, aUser);
            if (preference.isPresent()) {
                var json = preference.get().getTraits();
                var result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
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
    public <T extends PreferenceValue> void saveTraitsForUser(PreferenceKey<T> aKey, User aUser,
            T aTraits)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");
        requireNonNull(aTraits, "Parameter [traits] must be specified");

        try {
            var preference = getRawUserPreference(aKey, aUser).orElseGet(UserPreference::new);
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

    private <T extends PreferenceValue> Optional<UserPreference> getRawUserPreference(
            PreferenceKey<T> aKey, User aUser)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");

        var query = String.join("\n", //
                "FROM UserPreference ", //
                "WHERE user = :user ", //
                "AND name = :name");

        var name = aKey.getName();
        try {
            var pref = entityManager.createQuery(query, UserPreference.class) //
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
    public <T extends PreferenceValue> Optional<T> loadOptionalTraitsForUserAndProject(
            PreferenceKey<T> aKey, User aUser, Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        try {
            var pref = getUserProjectPreference(aKey, aUser, aProject);
            if (pref.isPresent()) {
                var json = pref.get().getTraits();
                T result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
                LOG.debug("Loaded preferences for key {} and user {} and project {}: [{}]", aKey,
                        aUser, aProject, result);
                return Optional.of(result);
            }
            return Optional.empty();
        }
        catch (IOException e) {
            LOG.error("Error while loading traits, returning empty", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public <T extends PreferenceValue> T loadTraitsForUserAndProject(PreferenceKey<T> aKey,
            User aUser, Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        try {
            var pref = getUserProjectPreference(aKey, aUser, aProject);
            if (pref.isPresent()) {
                var json = pref.get().getTraits();
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
    public <T extends PreferenceValue> void saveTraitsForUserAndProject(PreferenceKey<T> aKey,
            User aUser, Project aProject, T aTraits)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");
        requireNonNull(aTraits, "Parameter [traits] must be specified");

        try {
            var preference = getUserProjectPreference(aKey, aUser, aProject)
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

    private <T extends PreferenceValue> Optional<UserProjectPreference> getUserProjectPreference(
            PreferenceKey<T> aKey, User aUser, Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aUser, "Parameter [user] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        var query = String.join("\n", //
                "FROM UserProjectPreference ", //
                "WHERE user = :user", //
                "AND project = :project", //
                "AND name = :name");

        try {
            var pref = entityManager //
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
        requireNonNull(aProject, "Parameter [project] must be specified");

        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(UserProjectPreference.class);
        var root = query.from(UserProjectPreference.class);

        query.select(root);
        query.where(builder.equal(root.get(UserProjectPreference_.project), aProject));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    @Transactional
    public void saveUserProjectPreference(UserProjectPreference aPreference)
    {
        requireNonNull(aPreference, "Parameter [preference] must be specified");

        entityManager.persist(aPreference);
    }

    @Override
    @Transactional
    public <T extends PreferenceValue> T loadDefaultTraitsForProject(PreferenceKey<T> aKey,
            Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        try {
            var pref = getDefaultProjectPreference(aKey, aProject);
            if (pref.isPresent()) {
                var json = pref.get().getTraits();
                var result = JSONUtil.fromJsonString(aKey.getTraitClass(), json);
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
    public <T extends PreferenceValue> void saveDefaultTraitsForProject(PreferenceKey<T> aKey,
            Project aProject, T aTraits)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        try {
            var preference = getDefaultProjectPreference(aKey, aProject)
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
    public <T extends PreferenceValue> void clearDefaultTraitsForProject(PreferenceKey<T> aKey,
            Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        var query = String.join("\n", //
                "DELETE DefaultProjectPreference ", //
                "WHERE project = :project", //
                "AND name = :name");

        entityManager.createQuery(query) //
                .setParameter("project", aProject) //
                .setParameter("name", aKey.getName()) //
                .executeUpdate();
    }

    private <T extends PreferenceValue> Optional<DefaultProjectPreference> getDefaultProjectPreference(
            PreferenceKey<T> aKey, Project aProject)
    {
        requireNonNull(aKey, "Parameter [key] must be specified");
        requireNonNull(aProject, "Parameter [project] must be specified");

        var query = String.join("\n", //
                "FROM DefaultProjectPreference ", //
                "WHERE project = :project", //
                "AND name = :name");

        try {
            var pref = entityManager //
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
        requireNonNull(aProject, "Parameter [project] must be specified");

        var query = String.join("\n", //
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
        requireNonNull(aPreference, "Parameter [preference] must be specified");

        entityManager.persist(aPreference);
    }

    /*
     * Use default constructor of aClass to create new instance of T
     */
    @SuppressWarnings("unchecked")
    private <T> T buildDefault(Class<T> aClass)
    {
        requireNonNull(aClass, "Parameter [class] must be specified");

        try {
            return aClass.getConstructor().newInstance();
        }
        catch (NoSuchMethodException e) {
            if (Map.class.isAssignableFrom(aClass)) {
                return (T) new LinkedHashMap<>();
            }

            return ExceptionUtils.wrapAndThrow(e);
        }
        catch (Exception e) {
            return ExceptionUtils.wrapAndThrow(e);
        }
    }
}
