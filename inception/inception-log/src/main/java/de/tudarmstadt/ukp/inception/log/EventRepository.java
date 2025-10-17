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
package de.tudarmstadt.ukp.inception.log;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.function.FailableConsumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public interface EventRepository
{
    static final String SERVICE_NAME = "eventRepository";

    void create(LoggedEvent... aEvents);

    <E extends Throwable> void forEachLoggedEvent(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer);

    Optional<Range> getLastEditRange(SourceDocument aDocument, String aDataOwner);

    List<LoggedEvent> listRecentActivity(Project aProject, String aDataOwner,
            Collection<String> aEventTypes, int aMaxSize);

    /**
     * @return recently logged events.
     * 
     * @param aDataOwner
     *            the user to list events for.
     * @param aMaxSize
     *            return this number of recent events or less
     */
    List<LoggedEvent> listRecentActivity(String aDataOwner, int aMaxSize);

    List<SummarizedLoggedEvent> summarizeEventsBySessionOwner(String aSessionOwner,
            Project aProject, Instant aNow, Instant aMinus);

    List<SummarizedLoggedEvent> summarizeEventsByDataOwner(String aDataOwner, Project aProject,
            Instant aFrom, Instant aTo);
}
