/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic response send from the server to the Brat client. Serves as base-class for responses to
 * specific commands.
 */
public class AjaxResponse
{
    private String action;
    private List<String> messages = new ArrayList<String>();

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
     */
    public String getAction()
    {
        return action;
    }

    /**
     * Set the action command for which this is a response.
     */
    public void setAction(String aAction)
    {
        action = aAction;
    }

    /**
     * Get feedback messages to be displayed to the user, e.g. success or failure messages.
     */
    public List<String> getMessages()
    {
        return messages;
    }

    /**
     * Set feedback messages to be displayed to the user, e.g. success or failure messages.
     */
    public void setMessages(List<String> aMessages)
    {
        messages = aMessages;
    }
}
