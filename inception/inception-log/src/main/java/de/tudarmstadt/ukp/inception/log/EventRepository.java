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

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.function.FailableConsumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

public interface EventRepository
{
    static final String SERVICE_NAME = "eventRepository";

    void create(LoggedEvent... aEvents);

    /**
     * @param aProject
     *            the project to query the events from
     * @param aUsername
     *            the user who generated the events
     * @param aEventType
     *            the type of event
     * @param aMaxSize
     *            the maximum number of events to return
     * @param aRecommenderId
     *            the recommender to which the events relate
     * @return logged events of the given type, user name, project and recommender id from the db.
     */
    List<LoggedEvent> listLoggedEventsForRecommender(Project aProject, String aUsername,
            String aEventType, int aMaxSize, long aRecommenderId);

    <E extends Throwable> void forEachLoggedEvent(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer);

    /**
     * @return logged events of the given types, user name and project for every document from the
     *         db.
     * @param aProject
     *            the project to query the events from
     * @param aUsername
     *            the user who generated the events
     * @param aEventType
     *            the type of event
     * @param aMaxSize
     *            the maximum number of events to return
     */
    List<LoggedEvent> listUniqueLoggedEventsForDoc(Project aProject, String aUsername,
            String[] aEventType, int aMaxSize);

    /**
     * @return logged events of the given type, user name, project and detail string from the db.
     * @param aProject
     *            the project to query the events from
     * @param aUsername
     *            the user who generated the events
     * @param aEventType
     *            the type of event
     * @param aMaxSize
     *            the maximum number of events to return
     * @param aDetail
     *            the detail pattern per SQL LIKE operator, e.g. {@code "%recommender%"} finds all
     *            events containing the string {@code "recommender"} in their detail
     */
    List<LoggedEvent> listLoggedEventsForDetail(Project aProject, String aUsername,
            String aEventType, int aMaxSize, String aDetail);

    List<LoggedEvent> listRecentActivity(Project aProject, String aUsername,
            Collection<String> aEventTypes, int aMaxSize);

    /**
     * @return recently logged events.
     * 
     * @param aUsername
     *            the user to list events for.
     * @param aMaxSize
     *            return this number of recent events or less
     */
    List<LoggedEvent> listRecentActivity(String aUsername, int aMaxSize);
}
