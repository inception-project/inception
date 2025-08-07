/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.GazeteerEntry;
import jakarta.persistence.EntityManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#gazeteerService}.
 * </p>
 */
public class GazeteerServiceImpl
    implements GazeteerService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;

    private final RepositoryProperties repositoryProperties;

    @Autowired
    public GazeteerServiceImpl(RepositoryProperties aRepositoryProperties,
            EntityManager aEntityManager)
    {
        repositoryProperties = aRepositoryProperties;
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public List<Gazeteer> listGazeteers(Recommender aRecommender)
    {
        var query = join("\n", //
                "FROM Gazeteer", //
                "WHERE recommender = :recommender ", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Gazeteer.class) //
                .setParameter("recommender", aRecommender) //
                .getResultList();
    }

    @Override
    @Transactional
    public void createOrUpdateGazeteer(Gazeteer aGazeteer)
    {
        try (var logCtx = withProjectLogger(aGazeteer.getRecommender().getProject())) {
            if (aGazeteer.getId() == null) {
                entityManager.persist(aGazeteer);

                LOG.info("Created gazeteer [{}] for recommender {} in project {}",
                        aGazeteer.getName(), aGazeteer.getRecommender(),
                        aGazeteer.getRecommender().getProject());
            }
            else {
                entityManager.merge(aGazeteer);

                LOG.info("Updated gazeteer [{}] for recommender {} in project {}",
                        aGazeteer.getName(), aGazeteer.getRecommender(),
                        aGazeteer.getRecommender().getProject());
            }
        }
    }

    @Override
    @Transactional
    public void importGazeteerFile(Gazeteer aGazeteer, InputStream aStream) throws IOException
    {
        var gazFile = getGazeteerFile(aGazeteer);

        if (!gazFile.getParentFile().exists()) {
            gazFile.getParentFile().mkdirs();
        }

        try (var os = new FileOutputStream(gazFile)) {
            IOUtils.copyLarge(aStream, os);
        }
    }

    @Override
    public File getGazeteerFile(Gazeteer aGazeteer) throws IOException
    {
        return repositoryProperties.getPath().toPath() //
                .resolve("project") //
                .resolve(String.valueOf(aGazeteer.getRecommender().getProject().getId())) //
                .resolve("gazeteer") //
                .resolve(aGazeteer.getId() + ".txt") //
                .toFile();
    }

    @Override
    @Transactional
    public void deleteGazeteers(Gazeteer aGazeteer) throws IOException
    {
        try (var logCtx = withProjectLogger(aGazeteer.getRecommender().getProject())) {
            entityManager.remove(entityManager.contains(aGazeteer) //
                    ? aGazeteer //
                    : entityManager.merge(aGazeteer));

            var gaz = getGazeteerFile(aGazeteer);
            if (gaz.exists()) {
                gaz.delete();
            }

            LOG.info("Removed gazeteer [{}] for recommender {} in project {}", aGazeteer.getName(),
                    aGazeteer.getRecommender(), aGazeteer.getRecommender().getProject());
        }
    }

    @Override
    public List<GazeteerEntry> readGazeteerFile(Gazeteer aGaz) throws IOException
    {
        var file = getGazeteerFile(aGaz);

        var data = new ArrayList<GazeteerEntry>();

        try (var is = new FileInputStream(file)) {
            parseGazeteer(aGaz, is, data);
        }

        return data;
    }

    public void parseGazeteer(Gazeteer aGaz, InputStream aStream, List<GazeteerEntry> aTarget)
        throws IOException
    {
        int lineNumber = 0;
        var i = IOUtils.lineIterator(aStream, UTF_8);
        while (i.hasNext()) {
            lineNumber++;
            var line = i.nextLine().trim();

            if (line.isEmpty() || line.startsWith("#")) {
                // Ignore comment lines and empty lines
                continue;
            }

            var fields = line.split("\t");
            if (fields.length >= 2) {
                var text = trimToNull(fields[0]);
                var label = trimToNull(fields[1]);
                if (label != null && text != null) {
                    aTarget.add(new GazeteerEntry(text, label));
                }
            }
            else {
                throw new IOException("Unable to parse line " + lineNumber + " of ["
                        + aGaz.getName() + "] - no tab character found: [" + line + "]");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsGazeteer(Recommender aRecommender, String aName)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");
        Validate.notNull(aName, "Gazeteer name must be specified");

        var query = "SELECT COUNT(*) " + //
                "FROM Gazeteer " + //
                "WHERE recommender = :recommender AND name = :name";
        var count = entityManager.createQuery(query, Long.class)
                .setParameter("recommender", aRecommender).setParameter("name", aName)
                .getSingleResult();

        return count > 0;
    }
}
