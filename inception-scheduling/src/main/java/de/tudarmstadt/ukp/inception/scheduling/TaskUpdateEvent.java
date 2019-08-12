/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.scheduling;

import java.util.Optional;

import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.springframework.context.ApplicationEvent;

public class TaskUpdateEvent extends ApplicationEvent implements IWebSocketPushMessage
{
    private static final long serialVersionUID = -805305485165653941L;
    
    private final String username;
    private final double progress;
    private final TaskState state;
    private final String errorMsg;
    
    public TaskUpdateEvent(Object aSource, String aUserName, TaskState aState, double aProgress)
    {
        this(aSource, aUserName, aState, aProgress, null);
    }
    
    public TaskUpdateEvent(Object aSource, String aUserName, TaskState aState, double aProgress,
            String aErrorMsg)
    {
        super(aSource);
        username = aUserName;
        state = aState;
        progress = aProgress;
        errorMsg = aErrorMsg;
    }

    public String getUser()
    {
        return username;
    }

    public double getProgress()
    {
        return progress;
    }

    public TaskState getState()
    {
        return state;
    }

    public Optional<String> getErrorMsg()
    {
        return Optional.ofNullable(errorMsg);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TaskUpdateEvent [");
        builder.append(infoToString());
        builder.append(", state=");
        builder.append(state);
        builder.append("]");
        return builder.toString();
    }

    protected String infoToString()
    {
        StringBuilder builder = new StringBuilder();
        if (errorMsg != null) {
            builder.append("error=\"");
            builder.append(errorMsg);
            builder.append("\", ");
        }
        builder.append("user=");
        builder.append(username);
        builder.append(", ");
        builder.append("progress=");
        builder.append(progress);
        return builder.toString();
    }
    
}
