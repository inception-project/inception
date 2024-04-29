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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.casdoctor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class LogMessageSet
    implements Serializable
{
    private static final long serialVersionUID = 997324549494420840L;

    private String name;
    private List<LogMessage> messages = new ArrayList<>();

    public LogMessageSet(String aName)
    {
        name = aName;
    }

    public String getName()
    {
        return name;
    }

    public List<LogMessage> getMessages()
    {
        return messages;
    }

    public void add(LogMessage aMessage)
    {
        messages.add(aMessage);
    }
}
