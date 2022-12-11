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

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class RRecommenderLogMessage
    implements Serializable
{
    private static final long serialVersionUID = 4187626919775469472L;

    private static final String MESSAGE = "message";
    private static final String LEVEL = "level";

    private final LogLevel level;
    private final String message;

    public RRecommenderLogMessage(LogMessage aMessage)
    {
        level = aMessage.getLevel();
        message = aMessage.getMessage();
    }

    @JsonCreator
    public RRecommenderLogMessage(@JsonProperty(LEVEL) LogLevel aLevel,
            @JsonProperty(MESSAGE) String aMessage)
    {
        level = aLevel;
        message = aMessage;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append(LEVEL, level)
                .append(MESSAGE, message).toString();
    }

}
