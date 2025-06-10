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

import java.util.Collection;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@JsonInclude(value = Include.NON_NULL)
public record RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel level,
        @JsonProperty(MESSAGE) String message,
        @JsonProperty(value = ADD_MARKER_CLASSES) Collection<String> markerClassesToAdd,
        @JsonProperty(value = REMOVE_MARKER_CLASSES) Collection<String> markerClassesToRemove)
{

    private static final String MESSAGE = "message";
    private static final String LEVEL = "level";
    private static final String ADD_MARKER_CLASSES = "addClasses";
    private static final String REMOVE_MARKER_CLASSES = "removeClasses";

    public RRecommenderLogMessage(LogMessage aMessage)
    {
        this(aMessage.getLevel(), aMessage.getMessage(), null, null);
    }

    public RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel aLevel,
            @JsonProperty(MESSAGE) String aMessage)
    {
        this(aLevel, aMessage, null, null);
    }

    @JsonCreator
    public RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel level,
            @JsonProperty(MESSAGE) String message,
            @JsonProperty(value = ADD_MARKER_CLASSES) Collection<String> markerClassesToAdd,
            @JsonProperty(value = REMOVE_MARKER_CLASSES) Collection<String> markerClassesToRemove)
    {
        this.level = level;
        this.message = message;

        if (markerClassesToAdd == null) {
            this.markerClassesToAdd = emptySet();
        }
        else {
            this.markerClassesToAdd = markerClassesToAdd.stream().collect(toSet());
        }

        if (markerClassesToRemove == null) {
            this.markerClassesToRemove = emptySet();
        }
        else {
            this.markerClassesToRemove = markerClassesToRemove.stream().collect(toSet());
        }
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append(LEVEL, level)
                .append(MESSAGE, message).toString();
    }
}
