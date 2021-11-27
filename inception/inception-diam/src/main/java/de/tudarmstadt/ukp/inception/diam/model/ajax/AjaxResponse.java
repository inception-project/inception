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
package de.tudarmstadt.ukp.inception.diam.model.ajax;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A generic response send from the server to the Brat client. Serves as base-class for responses to
 * specific commands.
 */
public abstract class AjaxResponse
{
    private String action;
    private @JsonInclude(NON_EMPTY) List<String> messages = new ArrayList<>();

    public AjaxResponse()
    {
        // Nothing to do
    }

    public AjaxResponse(String aAction)
    {
        super();
        action = aAction;
    }

    public AjaxResponse(String aAction, List<String> aMessages)
    {
        super();
        action = aAction;
        messages = aMessages;
    }

    /**
     * Get the action command for which this is a response.
     * 
     * @return the action.
     */
    public String getAction()
    {
        return action;
    }

    /**
     * Set the action command for which this is a response.
     * 
     * @param aAction
     *            the action.
     */
    public void setAction(String aAction)
    {
        action = aAction;
    }

    /**
     * Get feedback messages to be displayed to the user, e.g. success or failure messages.
     * 
     * @return the messages.
     */
    public List<String> getMessages()
    {
        return messages;
    }

    /**
     * Set feedback messages to be displayed to the user, e.g. success or failure messages.
     * 
     * @param aMessages
     *            the messages.
     */
    public void setMessages(List<String> aMessages)
    {
        messages = aMessages;
    }
}
