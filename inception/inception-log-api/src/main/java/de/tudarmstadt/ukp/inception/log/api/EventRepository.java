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
package de.tudarmstadt.ukp.inception.log.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.function.FailableConsumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.UserSessionStats;

public interface EventRepository
{
    static final String SERVICE_NAME = "eventRepository";

    <E extends Throwable> void forEachLoggedEvent(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer);

    <E extends Throwable> void forEachLoggedEventUpdatable(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer);

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

    UserSessionStats getAggregateSessionDuration(String aSessionOwner);

    /**
     * Calculates historical document state counts backwards from current state to the specified
     * time using backwards replay of events. Returns one data point per day where counts changed.
     *
     * @param aProject
     *            the project
     * @param aCurrentStats
     *            current document state counts (starting point for backwards calculation)
     * @param aFrom
     *            earliest time to calculate backwards to
     * @return list of historical state snapshots, ordered oldest-first
     */
    List<DocumentStateSnapshot> calculateHistoricalDocumentStates(Project aProject,
            Map<SourceDocumentState, Long> aCurrentStats, Instant aFrom);

    /**
     * Represents document state counts at a specific point in time.
     */
    record DocumentStateSnapshot(Instant day, Map<SourceDocumentState, Integer> counts) {}
}
