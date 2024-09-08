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
package de.tudarmstadt.ukp.inception.recommendation.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.wicket.event.HybridApplicationUIEvent;

public class RecommendersResumedEvent
    extends ApplicationEvent
    implements HybridApplicationUIEvent
{
    private static final long serialVersionUID = -4736560772442881663L;

    private final Project project;
    private final String sessionOwner;

    public RecommendersResumedEvent(Object source, Project aProject, String aSessionOwner)
    {
        super(source);
        project = aProject;
        sessionOwner = aSessionOwner;
    }

    public Project getProject()
    {
        return project;
    }

    public String getUser()
    {
        return sessionOwner;
    }
}
