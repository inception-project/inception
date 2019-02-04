package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;

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
    public Map<String, String> readGazeteerFile(Gazeteer aGaz) throws IOException
    {
        File file = getGazeteerFile(aGaz);
        
        Map<String, String> data = new HashMap<>();
        
        try (InputStream is = new FileInputStream(file)) {
            LineIterator i = IOUtils.lineIterator(is, UTF_8);
            while (i.hasNext()) {
                String[] line = i.nextLine().split("\t", 2);
                if (line.length == 2) {
                    String label = trimToNull(line[0]);
                    String text = trimToNull(line[1]);
                    if (label != null && text != null) {
                        data.put(text, label);
                    }
                }
            }
        }
        
        return data;
    }
}
