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

import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class RecommenderTaskNotificationEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 777340980838549414L;

    private final Project project;
    private final String user;
    private final LogMessage message;

    protected RecommenderTaskNotificationEvent(Builder builder)
    {
        super(builder.source);

        project = builder.project;
        user = builder.user;
        message = builder.message;

        Validate.notNull(project, "Must specify project");
        Validate.notNull(user, "Must specify user");
    }

    public RecommenderTaskNotificationEvent(Object aSource, Project aProject, String aUser)
    {
        this(aSource, aProject, aUser, null);
    }

    public RecommenderTaskNotificationEvent(Object aSource, Project aProject, String aUser,
            LogMessage aMessage)
    {
        super(aSource);
        user = aUser;
        message = aMessage;
        project = aProject;
    }

    public LogMessage getMessage()
    {
        return message;
    }

    public String getUser()
    {
        return user;
    }

    public Project getProject()
    {
        return project;
    }

    public static Builder builder(Object aSource, Project aProject, String aUser)
    {
        return new Builder(aSource, aProject, aUser);
    }

    public static class Builder
    {
        protected final Object source;
        protected final Project project;
        protected final String user;
        protected LogMessage message;

        protected Builder(Object aSource, Project aProject, String aUser)
        {
            source = aSource;
            project = aProject;
            user = aUser;
        }

        public Builder withMessage(LogMessage aMessage)
        {
            message = aMessage;
            return this;
        }

        public RecommenderTaskNotificationEvent build()
        {
            return new RecommenderTaskNotificationEvent(this);
        }
    }
}
