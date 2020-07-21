/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.workload.dynamic.extensions;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.DefaultWorkflowActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkflowProperties;

@Order(1100)
@Component
public class DynamicWorkflowActionBarExtension
    implements ActionBarExtension
{
    private @Autowired WorkflowProperties workflowProperties;


    private final @PersistenceContext EntityManager entityManager;

    @Autowired
    public DynamicWorkflowActionBarExtension(EntityManager aEntityManager) {
        entityManager = aEntityManager;
    }

    @Override
    public String getRole()
    {
        return DefaultWorkflowActionBarExtension.class.getName();
    }

    @Override
    public int getPriority()
    {
        return 1;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage) {
        // New dynamic workflow only used when the new workflow manager selected in the settings.
        // Otherwise use the default one and skip this
        return workflowProperties.isWorkflowManagerActive();
    }

    @Override
    public Panel createActionBarItem(String aID, AnnotationPageBase aAnnotationPageBase)
    {
        return new DynamicAnnotatorWorkflowActionBarItemGroup(
            aID, aAnnotationPageBase, entityManager);
    }
}
