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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.scheduling.Task;

public abstract class RecommendationTask_ImplBase
    extends Task
{
    private final List<LogMessage> logMessages = new ArrayList<>();

    public RecommendationTask_ImplBase(Project aProject, String aTrigger)
    {
        super(aProject, aTrigger);
    }

    public RecommendationTask_ImplBase(User aUser, Project aProject, String aTrigger)
    {
        super(aUser, aProject, aTrigger);
    }

    public void inheritLog(List<LogMessage> aLogMessages)
    {
        logMessages.addAll(aLogMessages);
    }

    public void inheritLog(RecommendationTask_ImplBase aOther)
    {
        logMessages.addAll(aOther.logMessages);
    }

    public List<LogMessage> getLogMessages()
    {
        return unmodifiableList(logMessages);
    }

    public void info(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.info(this, aFormat, aValues));
    }

    public void warn(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.warn(this, aFormat, aValues));
    }

    public void error(String aFormat, Object... aValues)
    {
        logMessages.add(LogMessage.error(this, aFormat, aValues));
    }
}
