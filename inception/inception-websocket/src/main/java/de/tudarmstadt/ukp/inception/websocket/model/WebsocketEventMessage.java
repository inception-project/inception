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
package de.tudarmstadt.ukp.inception.websocket.model;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

public class WebsocketEventMessage
{
    private long timestamp;
    private String eventMsg;
    private String eventType;

    public WebsocketEventMessage()
    {
        // Nothing to do
    }

    public WebsocketEventMessage(Date aCreationDate, String aEventType)
    {
        this(aCreationDate.getTime(), aEventType);
    }

    public WebsocketEventMessage(long aTimestamp, String aEventType)
    {
        timestamp = aTimestamp;
        eventType = aEventType;
    }

    public WebsocketEventMessage(String aMsg, String aEventType,
            long aTime)
    {
        this(aTime, aEventType);
        eventMsg = aMsg;
    }


    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long aCreationDate)
    {
        timestamp = aCreationDate;
    }

    public String getEventMsg()
    {
        return eventMsg;
    }

    public void setEventMsg(String aEventMsg)
    {
        eventMsg = aEventMsg;
    }
    
    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String aEventType)
    {
        eventType = aEventType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        builder.append("eventType=");
        builder.append(eventType);
        if (StringUtils.isNotBlank(getEventMsg())) {
            builder.append(", eventMsg=[");
            builder.append(getEventMsg());
            builder.append("] ");
        }
        builder.append("]");
        return builder.toString();
    }
}
