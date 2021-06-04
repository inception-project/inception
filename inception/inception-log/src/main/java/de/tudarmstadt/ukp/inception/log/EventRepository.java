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
import java.util.function.Consumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

public interface EventRepository
{
    static final String SERVICE_NAME = "eventRepository";

    void create(LoggedEvent... aEvents);

    /**
     * Get the aMaxSize amount of logged events of the given type, user name, project and
     * recommender id from the db.
     */
    List<LoggedEvent> listLoggedEventsForRecommender(Project aProject, String aUsername,
            String aEventType, int aMaxSize, long aRecommenderId);

    void forEachLoggedEvent(Project aProject, Consumer<LoggedEvent> aConsumer);

    /**
     * Get the aMaxSize amount of logged events of the given types, user name and project for every
     * document from the db.
     */
    List<LoggedEvent> listUniqueLoggedEventsForDoc(Project aProject, String aUsername,
            String[] aEventType, int aMaxSize);

    /**
     * Get the aMaxSize amount of logged events of the given type, user name, project and detail
     * string from the db.
     */
    List<LoggedEvent> listLoggedEventsForDetail(Project aProject, String aUsername,
            String aEventType, int aMaxSize, String aDetail);

    List<LoggedEvent> listRecentActivity(Project aProject, String aUsername,
            Collection<String> aEventTypes, int aMaxSize);
    
    /**
     * List recently logged events that are not of the given types 
     * @param aNotTheseEventTypes these types will not be included
     * @param aMaxSize return this number of recent events or less
     */
    List<LoggedEvent> listFilteredRecentActivity(Collection<String> aNotTheseEventTypes, int aMaxSize);
}
