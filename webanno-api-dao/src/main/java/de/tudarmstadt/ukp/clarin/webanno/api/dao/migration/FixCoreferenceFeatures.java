/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.migration;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_TYPE_FEATURE;

import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * WebAnno versions until 3.0.0 set the feature types of Coreference features "referenceType"
 * and "referenceRelation" wrongly to "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference" - 
 * it should be "uima.cas.String"
 */
public class FixCoreferenceFeatures
    implements SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    private @PersistenceContext EntityManager entityManager;
    private @Autowired PlatformTransactionManager txManager;

    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public void start()
    {
        running = true;
        doMigration();
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public int getPhase()
    {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public void stop(Runnable aCallback)
    {
        stop();
        aCallback.run();
    }

    private void doMigration()
    {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("migrationRoot");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        
        TransactionStatus status = null;
        try {
            status = txManager.getTransaction(def);
            Query q = entityManager.createQuery(
                    "UPDATE AnnotationFeature \n" +
                    "SET type = :fixedType \n" +
                    "WHERE type = :oldType \n" +
                    "AND name in (:featureNames)");
            
            // This condition cannot be applied: 
            //   "AND layer.type = :layerType"
            //   q.setParameter("layerType", CHAIN_TYPE);
            // http://stackoverflow.com/questions/16506759/hql-is-generating-incomplete-cross-join-on-executeupdate
            // However, the risk that the migration upgrades the wrong featuers is still very low
            // even without this additional condition
            
            q.setParameter("featureNames",
                    Arrays.asList(COREFERENCE_RELATION_FEATURE, COREFERENCE_TYPE_FEATURE));
            q.setParameter("oldType", "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
            q.setParameter("fixedType", CAS.TYPE_NAME_STRING);
            int changed = q.executeUpdate();
            if (changed > 0) {
                log.info("DATABASE UPGRADE PERFORMED: [{}] coref chain features had their "
                        + "type fixed.", changed);
            }
            txManager.commit(status);
        }
        finally {
            if (status != null && !status.isCompleted()) {
                txManager.rollback(status);
            }
        }
    }
}
