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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer;

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
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.Gazetteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.GazetteerEntry;
import jakarta.persistence.EntityManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#gazetteerService}.
 * </p>
 */
public class GazetteerServiceImpl
    implements GazetteerService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;

    private final RepositoryProperties repositoryProperties;

    @Autowired
    public GazetteerServiceImpl(RepositoryProperties aRepositoryProperties,
            EntityManager aEntityManager)
    {
        repositoryProperties = aRepositoryProperties;
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public List<Gazetteer> listGazetteers(Recommender aRecommender)
    {
        var query = join("\n", //
                "FROM Gazetteer", //
                "WHERE recommender = :recommender ", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, Gazetteer.class) //
                .setParameter("recommender", aRecommender) //
                .getResultList();
    }

    @Override
    @Transactional
    public void createOrUpdateGazetteer(Gazetteer aGazetteer)
    {
        try (var logCtx = withProjectLogger(aGazetteer.getRecommender().getProject())) {
            if (aGazetteer.getId() == null) {
                entityManager.persist(aGazetteer);

                LOG.info("Created gazetteer [{}] for recommender {} in project {}",
                        aGazetteer.getName(), aGazetteer.getRecommender(),
                        aGazetteer.getRecommender().getProject());
            }
            else {
                entityManager.merge(aGazetteer);

                LOG.info("Updated gazetteer [{}] for recommender {} in project {}",
                        aGazetteer.getName(), aGazetteer.getRecommender(),
                        aGazetteer.getRecommender().getProject());
            }
        }
    }

    @Override
    @Transactional
    public void importGazetteerFile(Gazetteer aGazetteer, InputStream aStream) throws IOException
    {
        var gazFile = getGazetteerFile(aGazetteer);

        if (!gazFile.getParentFile().exists()) {
            gazFile.getParentFile().mkdirs();
        }

        try (var os = new FileOutputStream(gazFile)) {
            IOUtils.copyLarge(aStream, os);
        }
    }

    @Override
    public File getGazetteerFile(Gazetteer aGazetteer) throws IOException
    {
        return repositoryProperties.getPath().toPath() //
                .resolve("project") //
                .resolve(String.valueOf(aGazetteer.getRecommender().getProject().getId())) //
                .resolve("gazeteer") // historic folder name typo - fix later
                .resolve(aGazetteer.getId() + ".txt") //
                .toFile();
    }

    @Override
    @Transactional
    public void deleteGazetteers(Gazetteer aGazetteer) throws IOException
    {
        try (var logCtx = withProjectLogger(aGazetteer.getRecommender().getProject())) {
            entityManager.remove(entityManager.contains(aGazetteer) //
                    ? aGazetteer //
                    : entityManager.merge(aGazetteer));

            var gaz = getGazetteerFile(aGazetteer);
            if (gaz.exists()) {
                gaz.delete();
            }

            LOG.info("Removed gazetteer [{}] for recommender {} in project {}",
                    aGazetteer.getName(), aGazetteer.getRecommender(),
                    aGazetteer.getRecommender().getProject());
        }
    }

    @Override
    public List<GazetteerEntry> readGazetteerFile(Gazetteer aGaz) throws IOException
    {
        var file = getGazetteerFile(aGaz);

        var data = new ArrayList<GazetteerEntry>();

        try (var is = new FileInputStream(file)) {
            parseGazetteer(aGaz, is, data);
        }

        return data;
    }

    public void parseGazetteer(Gazetteer aGaz, InputStream aStream, List<GazetteerEntry> aTarget)
        throws IOException
    {
        int lineNumber = 0;
        var i = IOUtils.lineIterator(aStream, UTF_8);
        while (i.hasNext()) {
            lineNumber++;
            var line = i.next().trim();

            if (line.isEmpty() || line.startsWith("#")) {
                // Ignore comment lines and empty lines
                continue;
            }

            var fields = line.split("\t");
            if (fields.length >= 2) {
                var text = trimToNull(fields[0]);
                var label = trimToNull(fields[1]);
                if (label != null && text != null) {
                    aTarget.add(new GazetteerEntry(text, label));
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
    public boolean existsGazetteer(Recommender aRecommender, String aName)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");
        Validate.notNull(aName, "Gazetteer name must be specified");

        var query = "SELECT COUNT(*) " + //
                "FROM Gazetteer " + //
                "WHERE recommender = :recommender AND name = :name";
        var count = entityManager.createQuery(query, Long.class)
                .setParameter("recommender", aRecommender) //
                .setParameter("name", aName) //
                .getSingleResult();

        return count > 0;
    }
}
