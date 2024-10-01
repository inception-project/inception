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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsProperties;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

public class ConstraintsServiceImpl
    implements ConstraintsService
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @PersistenceContext EntityManager entityManager;
    private @Autowired RepositoryProperties repositoryProperties;

    private final ConstraintsProperties properties;
    private final LoadingCache<Project, ParsedConstraints> constraintsCache;

    public ConstraintsServiceImpl(ConstraintsProperties aProperties)
    {
        properties = aProperties;
        constraintsCache = createContstaintsCache(properties);
    }

    private LoadingCache<Project, ParsedConstraints> createContstaintsCache(
            ConstraintsProperties aProperties)
    {
        var queryCacheBuilder = Caffeine.newBuilder() //
                .expireAfterAccess(aProperties.getCacheExpireDelay());

        if (LOG.isTraceEnabled()) {
            queryCacheBuilder.recordStats();
        }

        return queryCacheBuilder.build(this::loadConstraints);
    }

    @EventListener
    public void onAfterProjectRemoved(AfterProjectRemovedEvent aEvent)
    {
        flushCache(aEvent.getProject());
    }

    @Override
    @Transactional
    public List<ConstraintSet> listConstraintSets(Project aProject)
    {
        return entityManager
                .createQuery("FROM ConstraintSet WHERE project = :project ORDER BY name ASC ",
                        ConstraintSet.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public void createOrUpdateConstraintSet(ConstraintSet aSet)
    {
        Validate.notNull(aSet, "Constraints set must be specified");

        try (var logCtx = withProjectLogger(aSet.getProject())) {
            if (isNull(aSet.getId())) {
                entityManager.persist(aSet);
                LOG.info("Created constraints set [{}] in project {}", aSet.getName(),
                        aSet.getProject());
            }
            else {
                entityManager.merge(aSet);
                LOG.info("Updated constraints set [{}] in project {}", aSet.getName(),
                        aSet.getProject());
            }
        }
    }

    @Override
    @Transactional
    public void removeConstraintSet(ConstraintSet aSet)
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            entityManager.remove(entityManager.merge(aSet));

            LOG.info("Removed constraints set [{}] in project {}", aSet.getName(),
                    aSet.getProject());
        }

        flushCache(aSet.getProject());
    }

    @Override
    public String readConstrainSet(ConstraintSet aSet) throws IOException
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            var constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            var filename = aSet.getId() + ".txt";

            String data;
            try (var is = new BOMInputStream(
                    new FileInputStream(new File(constraintRulesPath, filename)))) {
                data = IOUtils.toString(is, UTF_8);
            }

            LOG.debug("Read constraints set [{}] in project {}", aSet.getName(), aSet.getProject());

            return data;
        }
    }

    @Override
    public void writeConstraintSet(ConstraintSet aSet, InputStream aContent) throws IOException
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            var constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            var filename = aSet.getId() + ".txt";
            FileUtils.forceMkdir(new File(constraintRulesPath));
            FileUtils.copyInputStreamToFile(aContent, new File(constraintRulesPath, filename));

            LOG.info("Saved constraints set [{}] in project {}", aSet.getName(), aSet.getProject());
        }

        flushCache(aSet.getProject());
    }

    /**
     * Provides exporting constraints as a file.
     */
    @Override
    public File exportConstraintAsFile(ConstraintSet aSet)
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            var constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            var filename = aSet.getId() + ".txt";
            var constraintsFile = new File(constraintRulesPath, filename);
            if (constraintsFile.exists()) {
                LOG.info("Exported constraints set [{}] from project {}", aSet.getName(),
                        aSet.getProject());
                return constraintsFile;
            }
            else {
                LOG.error("Unable to read constraints set file [{}] in project {}", filename,
                        aSet.getProject());
                return null;
            }
        }
    }

    /**
     * Checks if there's a constraint set already with the name
     * 
     * @param constraintSetName
     *            The name of constraint set
     * @return true if exists
     */
    @Override
    public boolean existConstraintSet(String constraintSetName, Project aProject)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM ConstraintSet WHERE project = :project" + " AND name = :name ",
                            ConstraintSet.class)
                    .setParameter("project", aProject) //
                    .setParameter("name", constraintSetName) //
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    private void flushCache(Project aProject)
    {
        // Drop cached results from the KB being updated
        constraintsCache.asMap().keySet()
                .removeIf(key -> Objects.equals(key.getId(), aProject.getId()));
    }

    @Override
    public ParsedConstraints getMergedConstraints(Project aProject)
    {
        return constraintsCache.get(aProject);
    }

    private ParsedConstraints loadConstraints(Project aProject) throws IOException, ParseException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            var sets = new ArrayList<ParsedConstraints>();
            for (var set : listConstraintSets(aProject)) {
                sets.add(loadConstraints(set));
            }

            return mergeConstraintSets(sets);
        }
    }

    static ParsedConstraints mergeConstraintSets(List<ParsedConstraints> sets) throws ParseException
    {
        var allImports = new LinkedHashMap<String, String>();
        var allScopes = new LinkedHashMap<String, List<Rule>>();

        for (var constraints : sets) {

            // Merge imports
            for (var e : constraints.getImports().entrySet()) {
                // Check if the value already points to some other feature in previous
                // constraint file(s).
                if (allImports.containsKey(e.getKey())
                        && !e.getValue().equalsIgnoreCase(allImports.get(e.getKey()))) {
                    // If detected, notify user with proper message and abort merging
                    var errorMessage = "Conflict detected in imports for key \"" + e.getKey()
                            + "\", conflicting values are \"" + e.getValue() + "\" & \""
                            + allImports.get(e.getKey())
                            + "\". Please contact a project manager to correct this."
                            + "Constraint may not work." + "\nAborting Constraint rules merge!";
                    throw new ParseException(errorMessage);
                }
            }
            allImports.putAll(constraints.getImports());

            // Merge scopes
            for (var scope : constraints.getScopes()) {
                var target = allScopes.computeIfAbsent(scope.getScopeName(),
                        $ -> new ArrayList<Rule>());
                target.addAll(scope.getRules());
            }
        }

        return new ParsedConstraints(allImports, allScopes);
    }

    private ParsedConstraints loadConstraints(ConstraintSet set) throws IOException, ParseException
    {
        var script = readConstrainSet(set);
        var parser = new ConstraintsParser(new StringReader(script));
        var astConstraintsSet = parser.constraintsSet();
        var constraints = new ParsedConstraints(astConstraintsSet);
        return constraints;
    }
}
