/*
 * Copyright 2017
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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;

import javax.persistence.NoResultException;

import org.apache.uima.jcas.cas.TOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * WebAnno 3.3.0 and 3.3.1 fail to set the "attach feature" during import. This fix takes care
 * of restoring the missing information by setting the "attach feature":
 * 
 * <ul>
 * <li>for "Dependency" layers to the "pos" feature of the "Token" layer
 * <li>for "POS" layers to the "pos" feature of the "Token" layer
 * <li>for "Lemma" layers to the "lemma" feature of the "Token" layer
 * <li>for "MorphologicalFeatures" layers to the "morph" feature of the "Token" layer
 * </ul>
 */
@Component
@Lazy(false)
public class FixAttachFeature330
    implements SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    private @Autowired PlatformTransactionManager txManager;
    private @Autowired ProjectService projectService;
    private @Autowired AnnotationSchemaService annotationSchemaService;

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

            for (Project project : projectService.listProjects()) {
                try {
                    AnnotationLayer tokenLayer = annotationSchemaService.findLayer(
                            project, Token.class.getName());
                    
                    // Set attach-feature of Dependency layer to Token.pos if necessary
                    fix(project, Dependency.class, RELATION_TYPE, tokenLayer, "pos");
    
                    // Set attach-feature of POS layer to Token.pos if necessary
                    fix(project, POS.class, SPAN_TYPE, tokenLayer, "pos");
    
                    // Set attach-feature of Lemma layer to Token.lemma if necessary
                    fix(project, Lemma.class, SPAN_TYPE, tokenLayer, "lemma");
    
                    // Set attach-feature of MorphologicalFeatures layer to Token.morph if necessary
                    fix(project, MorphologicalFeatures.class, SPAN_TYPE, tokenLayer, "morph");
                }
                catch (NoResultException e) {
                    // This only happens if a project is not fully set up. Every project
                    // should have a Token layer. However, it is not the responsibility of this
                    // migration to enforce this, so we just ignore it.
                    log.warn("Project {} does not seem to include a Token layer!", project);
                }
            }
            
            txManager.commit(status);
        }
        finally {
            if (status != null && !status.isCompleted()) {
                txManager.rollback(status);
            }
        }
    }
    
    private void fix(Project aProject, Class<? extends TOP> aLayer, String aLayerType,
            AnnotationLayer aTokenLayer, String aFeature)
    {
        if (annotationSchemaService.existsLayer(aLayer.getName(), aLayerType, aProject)) {
            AnnotationLayer layer = annotationSchemaService.findLayer(aProject, aLayer.getName());

            if (layer.getAttachFeature() == null) {
                layer.setAttachFeature(annotationSchemaService.getFeature(aFeature, aTokenLayer));
                log.info("DATABASE UPGRADE PERFORMED: Fixed attach-feature of layer "
                                + "[{}] ({}) in project [{}] ({})",
                        layer.getUiName(), layer.getId(), aProject.getName(), aProject.getId());
            }
        }
    }
}

