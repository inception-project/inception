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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.withProjectLogger;
import static java.util.Objects.isNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTConstraintsSet;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component(ConstraintsService.SERVICE_NAME)
public class ConstraintsServiceImpl
    implements ConstraintsService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private @Autowired RepositoryProperties repositoryProperties;

    public ConstraintsServiceImpl()
    {
        // Nothing to do
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
                log.info("Created constraints set [{}] in project {}", aSet.getName(),
                        aSet.getProject());
            }
            else {
                entityManager.merge(aSet);
                log.info("Updated constraints set [{}] in project {}", aSet.getName(),
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

            log.info("Removed constraints set [{}] in project {}", aSet.getName(),
                    aSet.getProject());
        }
    }

    @Override
    public String readConstrainSet(ConstraintSet aSet) throws IOException
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            String constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            String filename = aSet.getId() + ".txt";

            String data;
            try (BOMInputStream is = new BOMInputStream(
                    new FileInputStream(new File(constraintRulesPath, filename)))) {
                data = IOUtils.toString(is, "UTF-8");
            }

            log.debug("Read constraints set [{}] in project {}", aSet.getName(), aSet.getProject());

            return data;
        }
    }

    @Override
    public void writeConstraintSet(ConstraintSet aSet, InputStream aContent) throws IOException
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            String constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            String filename = aSet.getId() + ".txt";
            FileUtils.forceMkdir(new File(constraintRulesPath));
            FileUtils.copyInputStreamToFile(aContent, new File(constraintRulesPath, filename));

            log.info("Saved constraints set [{}] in project {}", aSet.getName(), aSet.getProject());
        }
    }

    /**
     * Provides exporting constraints as a file.
     */
    @Override
    public File exportConstraintAsFile(ConstraintSet aSet)
    {
        try (var logCtx = withProjectLogger(aSet.getProject())) {
            String constraintRulesPath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aSet.getProject().getId() + "/"
                    + ConstraintsService.CONSTRAINTS + "/";
            String filename = aSet.getId() + ".txt";
            File constraintsFile = new File(constraintRulesPath, filename);
            if (constraintsFile.exists()) {
                log.info("Exported constraints set [{}] from project {}", aSet.getName(),
                        aSet.getProject());
                return constraintsFile;
            }
            else {
                log.error("Unable to read constraints set file [{}] in project {}", filename,
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
                    .setParameter("project", aProject).setParameter("name", constraintSetName)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    public ParsedConstraints loadConstraints(Project aProject) throws IOException, ParseException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            ParsedConstraints merged = null;

            for (ConstraintSet set : listConstraintSets(aProject)) {
                String script = readConstrainSet(set);
                ConstraintsParser parser = new ConstraintsParser(new StringReader(script));
                ASTConstraintsSet astConstraintsSet = parser.constraintsSet();
                ParsedConstraints constraints = new ParsedConstraints(astConstraintsSet);

                if (merged == null) {
                    merged = constraints;
                }
                else {
                    // Merge imports
                    for (Entry<String, String> e : constraints.getImports().entrySet()) {
                        // Check if the value already points to some other feature in previous
                        // constraint file(s).
                        if (merged.getImports().containsKey(e.getKey()) && !e.getValue()
                                .equalsIgnoreCase(merged.getImports().get(e.getKey()))) {
                            // If detected, notify user with proper message and abort merging
                            String errorMessage = "Conflict detected in imports for key \""
                                    + e.getKey() + "\", conflicting values are \"" + e.getValue()
                                    + "\" & \"" + merged.getImports().get(e.getKey())
                                    + "\". Please contact Project Admin for correcting this."
                                    + "Constraints feature may not work."
                                    + "\nAborting Constraint rules merge!";
                            throw new ParseException(errorMessage);
                        }
                    }
                    merged.getImports().putAll(constraints.getImports());

                    // Merge scopes
                    for (Scope scope : constraints.getScopes()) {
                        Scope target = merged.getScopeByName(scope.getScopeName());
                        if (target == null) {
                            // Scope does not exist yet
                            merged.getScopes().add(scope);
                        }
                        else {
                            // Scope already exists
                            target.getRules().addAll(scope.getRules());
                        }
                    }
                }
            }

            return merged;
        }
    }
}
