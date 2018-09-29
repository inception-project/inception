/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(ConstraintsService.SERVICE_NAME)
public class ConstraintsServiceImpl
    implements ConstraintsService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Value(value = "${repository.path}")
    private File dir;

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
                        ConstraintSet.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public void createConstraintSet(ConstraintSet aSet)
    {
        entityManager.persist(aSet);
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aSet.getProject().getId()))) {
            log.info("Created constraints set [{}] in project [{}]({})",
                    aSet.getName(), aSet.getProject().getName(), aSet.getProject().getId());
        }
    }

    @Override
    @Transactional
    public void removeConstraintSet(ConstraintSet aSet)
    {
        entityManager.remove(entityManager.merge(aSet));
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aSet.getProject().getId()))) {
            log.info("Removed constraints set [{}] in project [{}]({})",
                    aSet.getName(), aSet.getProject().getName(), aSet.getProject().getId());
        }
    }

    @Override
    public String readConstrainSet(ConstraintSet aSet)
        throws IOException
    {
        String constraintRulesPath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aSet.getProject().getId() + "/" + ConstraintsService.CONSTRAINTS + "/";
        String filename = aSet.getId() + ".txt";
        
        String data;
        try (BOMInputStream is = new BOMInputStream(
                new FileInputStream(new File(constraintRulesPath, filename)))) {
            data = IOUtils.toString(is, "UTF-8");
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aSet.getProject().getId()))) {
            log.info("Read constraints set [{}] in project [{}]({})",
                    aSet.getName(), aSet.getProject().getName(), aSet.getProject().getId());
        }
        
        return data;
    }

    @Override
    public void writeConstraintSet(ConstraintSet aSet, InputStream aContent)
        throws IOException
    {
        String constraintRulesPath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aSet.getProject().getId() + "/" + ConstraintsService.CONSTRAINTS + "/";
        String filename = aSet.getId() + ".txt";
        FileUtils.forceMkdir(new File(constraintRulesPath));
        FileUtils.copyInputStreamToFile(aContent, new File(constraintRulesPath, filename));

        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aSet.getProject().getId()))) {
            log.info("Saved constraints set [{}] in project [{}]({})",
                    aSet.getName(), aSet.getProject().getName(), aSet.getProject().getId());
        }
    }
    
    /**
     * Provides exporting constraints as a file.
     */
    @Override
    public File exportConstraintAsFile(ConstraintSet aSet)
    {
        String constraintRulesPath = dir.getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aSet.getProject().getId() + "/" + ConstraintsService.CONSTRAINTS + "/";
        String filename = aSet.getId() + ".txt";
        File constraintsFile = new File(constraintRulesPath, filename);
        if (constraintsFile.exists()) {
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aSet.getProject().getId()))) {
                log.info("Exported constraints set [{}] from project [{}]({})",
                        aSet.getName(), aSet.getProject().getName(), aSet.getProject().getId());
            }
            return constraintsFile;
        }
        else {
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aSet.getProject().getId()))) {
                log.info("Unable to read constraints set file [{}] in project [{}]({})",
                        filename, aSet.getProject().getName(), aSet.getProject().getId());
            }
            return null;
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
            entityManager.createQuery("FROM ConstraintSet WHERE project = :project" 
                            + " AND name = :name ", ConstraintSet.class)
                    .setParameter("project", aProject).
                    setParameter("name", constraintSetName)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }        
    }
    
    @Override
    public ParsedConstraints loadConstraints(Project aProject)
            throws IOException, ParseException
    {
        ParsedConstraints merged = null;

        for (ConstraintSet set : listConstraintSets(aProject)) {
            String script = readConstrainSet(set);
            ConstraintsGrammar parser = new ConstraintsGrammar(new StringReader(script));
            Parse p = parser.Parse();
            ParsedConstraints constraints = p.accept(new ParserVisitor());

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
                        String errorMessage = "Conflict detected in imports for key \"" + e.getKey()
                                + "\", conflicting values are \"" + e.getValue() + "\" & \""
                                + merged.getImports().get(e.getKey())
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
