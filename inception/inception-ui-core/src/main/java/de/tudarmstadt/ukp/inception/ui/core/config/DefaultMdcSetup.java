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
package de.tudarmstadt.ukp.inception.ui.core.config;

import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;

import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;

public class DefaultMdcSetup
    implements AutoCloseable
{

    public DefaultMdcSetup(RepositoryProperties repositoryProperties, Project aProject, User aUser)
    {
        // We are in a new thread. Set up thread-specific MDC
        if (repositoryProperties != null) {
            MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        }

        if (aUser != null) {
            MDC.put(KEY_USERNAME, aUser.getUsername());
        }

        if (aProject != null) {
            MDC.put(KEY_PROJECT_ID, String.valueOf(aProject.getId()));
        }
    }

    @Override
    public void close()
    {
        MDC.remove(KEY_REPOSITORY_PATH);
        MDC.remove(KEY_USERNAME);
        MDC.remove(KEY_PROJECT_ID);
    }
}
