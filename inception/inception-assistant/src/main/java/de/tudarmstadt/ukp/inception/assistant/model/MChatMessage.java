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
package de.tudarmstadt.ukp.inception.assistant.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public sealed interface MChatMessage
    extends MMessage
    permits MTextMessage, MCallResponse
{
    UUID id();

    /**
     * @return the role of the message author
     */
    String role();

    /**
     * @return the role of the message actor
     */
    String actor();

    /**
     * @return if the message is part of the inner monolog, RAG or similarly not normally exposed to
     *         the user. Internal messages are recorded but only sent to the user in debug mode.
     */
    boolean internal();

    /**
     * @return if the message should disappear at once (i.e. it is not recorded)
     */
    @JsonIgnore
    boolean ephemeral();

    MPerformanceMetrics performance();

    UUID context();

    List<MReference> references();

    String textRepresentation();

    default String toolName()
    {
        return null;
    }

    default String thinking()
    {
        return null;
    }

    default boolean done()
    {
        return true;
    }
}
