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
package de.tudarmstadt.ukp.inception.recommendation.footer;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class RRecommenderLogMessage
    implements Serializable
{
    private static final long serialVersionUID = 4187626919775469472L;

    private static final String MESSAGE = "message";
    private static final String LEVEL = "level";
    private static final String ADD_MARKER_CLASSES = "addClasses";
    private static final String REMOVE_MARKER_CLASSES = "removeClasses";

    private final LogLevel level;
    private final String message;
    private final Set<String> markerClassesToRemove;
    private final Set<String> markerClassesToAdd;

    public RRecommenderLogMessage(LogMessage aMessage)
    {
        level = aMessage.getLevel();
        message = aMessage.getMessage();
        markerClassesToAdd = emptySet();
        markerClassesToRemove = emptySet();
    }

    public RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel aLevel,
            @JsonProperty(MESSAGE) String aMessage)
    {
        this(aLevel, aMessage, null, null);
    }

    @JsonCreator
    public RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel aLevel,
            @JsonProperty(MESSAGE) String aMessage,
            @JsonProperty(value = ADD_MARKER_CLASSES) Collection<String> aMarkerClassesToAdd,
            @JsonProperty(value = REMOVE_MARKER_CLASSES) Collection<String> aMarkerClassesToRemove)
    {
        level = aLevel;
        message = aMessage;

        if (aMarkerClassesToAdd == null) {
            markerClassesToAdd = emptySet();
        }
        else {
            markerClassesToAdd = aMarkerClassesToAdd.stream().collect(toSet());
        }

        if (aMarkerClassesToRemove == null) {
            markerClassesToRemove = emptySet();
        }
        else {
            markerClassesToRemove = aMarkerClassesToRemove.stream().collect(toSet());
        }
    }

    @JsonProperty(LEVEL)
    public LogLevel getLevel()
    {
        return level;
    }

    @JsonProperty(MESSAGE)
    public String getMessage()
    {
        return message;
    }

    @JsonProperty(ADD_MARKER_CLASSES)
    public Set<String> getMarkerClassesToAdd()
    {
        return markerClassesToAdd;
    }

    @JsonProperty(REMOVE_MARKER_CLASSES)
    public Set<String> getMarkerClassesToRemove()
    {
        return markerClassesToRemove;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append(LEVEL, level)
                .append(MESSAGE, message).toString();
    }
}
