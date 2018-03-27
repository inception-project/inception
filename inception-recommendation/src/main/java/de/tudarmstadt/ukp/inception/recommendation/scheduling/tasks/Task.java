/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.model.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.model.TaskType.Type;

public class Task implements Runnable
{
    private AnnotationSchemaService annoService;
    private Project project;
    private User user;
    private Consumer<AnnotationLayer>[] tasks;
    private Type type;

    @SafeVarargs
    public Task(AnnotationSchemaService aAnnoService, Project p, User u, Type t,
            Consumer<AnnotationLayer>... tasks)
    {
        this.annoService = aAnnoService;
        this.project = p;
        this.user = u;
        this.type = t;
        this.tasks = tasks;
    }

    @Override
    public void run()
    {
        annoService.listAnnotationLayer(project);
        List<AnnotationLayer> layers = new LinkedList<>();
        
        for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
            if (layer.isEnabled()) {
                layers.add(layer);    
            }
        }
        
        ClassificationToolRegistry registry = new ClassificationToolRegistry();
        Map<String, List<String>> classificationToolIds = registry
                .getRegisteredClassificationToolIds();
        StringBuilder sb = new StringBuilder();
        
        for (AnnotationLayer layer : layers) {                
            if (!classificationToolIds.containsKey(layer.getName())) {
                sb.append((sb.length() == 0 ? "" : ", ")).append(layer.getUiName());
                continue;
            }
            for (Consumer<AnnotationLayer> task : tasks) {
                task.accept(layer);
            }
        }
    }

    public User getUser()
    {
        return user;
    }

    public Type getType()
    {
        return type;
    }

    public Project getProject()
    {
        return project;
    }
}
