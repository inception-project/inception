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
package de.tudarmstadt.ukp.inception.assistant;

import de.tudarmstadt.ukp.inception.assistant.model.MCommandMessage;

/**
 * Interface for dispatching command messages from tools to the frontend. Tools can use this to
 * trigger UI updates or other commands without needing to know about the underlying WebSocket or
 * messaging infrastructure.
 */
public interface CommandDispatcher
{
    /**
     * Dispatch a command message to the frontend.
     * 
     * @param aCommand
     *            the command message to dispatch
     */
    void dispatch(MCommandMessage aCommand);
}
