/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.GazeteerEntry;

@Component
public class GazeteerServiceImpl
    implements GazeteerService
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final RepositoryProperties repositoryProperties;
    
    @Autowired
    public GazeteerServiceImpl(RepositoryProperties aRepositoryProperties)
    {
        repositoryProperties = aRepositoryProperties;
    }    

    public GazeteerServiceImpl(RepositoryProperties aRepositoryProperties,
            EntityManager aEntityManager)
    {
        this(aRepositoryProperties);
        entityManager = aEntityManager;
    }
    
    @Override
    @Transactional
    public List<Gazeteer> listGazeteers(Recommender aRecommender)
    {
        String query = String.join("\n", 
                "FROM Gazeteer",
                "WHERE recommender = :recommender ",
                "ORDER BY name ASC");
        
        return entityManager
                .createQuery(query, Gazeteer.class)
                .setParameter("recommender", aRecommender)
                .getResultList();
    }

    @Override
    @Transactional
    public void createOrUpdateGazeteer(Gazeteer aGazeteer)
    {
        if (aGazeteer.getId() == null) {
            entityManager.persist(aGazeteer);
            
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aGazeteer.getRecommender().getProject().getId()))) {
                log.info("Created gazeteer [{}] for recommender [{}]({}) in project [{}]({})",
                        aGazeteer.getName(), aGazeteer.getRecommender().getName(),
                        aGazeteer.getRecommender().getId(),
                        aGazeteer.getRecommender().getProject().getName(),
                        aGazeteer.getRecommender().getProject().getId());
            }
        }
        else {
            entityManager.merge(aGazeteer);
            
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aGazeteer.getRecommender().getProject().getId()))) {
                log.info("Updated gazeteer [{}] for recommender [{}]({}) in project [{}]({})",
                        aGazeteer.getName(), aGazeteer.getRecommender().getName(),
                        aGazeteer.getRecommender().getId(),
                        aGazeteer.getRecommender().getProject().getName(),
                        aGazeteer.getRecommender().getProject().getId());
            }
        }
    }

    @Override
    @Transactional
    public void importGazeteerFile(Gazeteer aGazeteer, InputStream aStream) throws IOException
    {
        File gazFile = getGazeteerFile(aGazeteer);
        
        if (!gazFile.getParentFile().exists()) {
            gazFile.getParentFile().mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(gazFile)) {
            IOUtils.copyLarge(aStream, os);
        }
    }

    @Override
    public File getGazeteerFile(Gazeteer aGazeteer) throws IOException
    {
        return repositoryProperties.getPath().toPath()
                .resolve("project")
                .resolve(String.valueOf(aGazeteer.getRecommender().getProject().getId()))
                .resolve("gazeteer")
                .resolve(aGazeteer.getId() + ".txt")
                .toFile();
    }

    @Override
    @Transactional
    public void deleteGazeteers(Gazeteer aGazeteer) throws IOException
    {
        entityManager.remove(
                entityManager.contains(aGazeteer) ? aGazeteer : entityManager.merge(aGazeteer));
        
        File gaz = getGazeteerFile(aGazeteer);
        if (gaz.exists()) {
            gaz.delete();
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aGazeteer.getRecommender().getProject().getId()))) {
            log.info("Removed gazeteer [{}] from recommender [{}]({}) in project [{}]({})",
                    aGazeteer.getName(), aGazeteer.getRecommender().getName(),
                    aGazeteer.getRecommender().getId(),
                    aGazeteer.getRecommender().getProject().getName(),
                    aGazeteer.getRecommender().getProject().getId());
        }
    }
    
    @Override
    public List<GazeteerEntry> readGazeteerFile(Gazeteer aGaz)
        throws IOException
    {
        File file = getGazeteerFile(aGaz);
        
        List<GazeteerEntry> data = new ArrayList<>();
        
        try (InputStream is = new FileInputStream(file)) {
            parseGazeteer(aGaz, is, data);
        }
        
        return data;
    }
    
    public void parseGazeteer(Gazeteer aGaz, InputStream aStream, List<GazeteerEntry> aTarget)
        throws IOException
    {
        int lineNumber = 0;
        LineIterator i = IOUtils.lineIterator(aStream, UTF_8);
        while (i.hasNext()) {
            lineNumber++;
            String line = i.nextLine().trim();
            
            if (line.isEmpty() || line.startsWith("#")) {
                // Ignore comment lines and empty lines
                continue;
            }
            
            String[] fields = line.split("\t");
            if (fields.length == 2) {
                String text = trimToNull(fields[0]);
                String label = trimToNull(fields[1]);
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
    @Transactional
    public boolean existsGazeteer(Recommender aRecommender, String aName)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");
        Validate.notNull(aName, "Gazeteer name must be specified");
        
        String query = 
                "SELECT COUNT(*) " +
                "FROM Gazeteer " + 
                "WHERE recommender = :recommender AND name = :name";
        
        long count = entityManager.createQuery(query, Long.class)
            .setParameter("recommender", aRecommender)
            .setParameter("name", aName)
            .getSingleResult();

        return count > 0;
    }
}
