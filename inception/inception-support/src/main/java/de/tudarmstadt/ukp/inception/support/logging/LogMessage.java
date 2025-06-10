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
package de.tudarmstadt.ukp.inception.support.logging;

import java.io.Serializable;
import java.util.Objects;

import org.apache.wicket.feedback.IFeedbackContributor;

public class LogMessage
    implements Serializable
{
    private static final long serialVersionUID = 2002139781814027105L;

    public final LogLevel level;
    public final String source;
    public final String message;

    public LogMessage(Object aSource, LogLevel aLevel, String aMessage)
    {
        this(aSource, aLevel, "%s", aMessage);
    }

    public LogMessage(Object aSource, LogLevel aLevel, String aFormat, Object... aValues)
    {
        super();
        if (aSource instanceof String) {
            source = (String) aSource;
        }
        else if (aSource instanceof Class) {
            source = ((Class<?>) aSource).getSimpleName();
        }
        else {
            source = aSource != null ? aSource.getClass().getSimpleName() : null;
        }
        level = aLevel;
        message = String.format(aFormat, aValues);
    }

    public LogLevel getLevel()
    {
        return level;
    }

    public String getMessage()
    {
        return message;
    }

    public String getSource()
    {
        return source;
    }

    public void toWicket(IFeedbackContributor aComponent)
    {
        switch (getLevel()) {
        case INFO:
            aComponent.info(getMessage());
            break;
        case WARN:
            aComponent.warn(getMessage());
            break;
        case ERROR:
            aComponent.error(getMessage());
            break;
        default:
            aComponent.error(getMessage());
            break;
        }
    }

    @Override
    public String toString()
    {
        return String.format("[%s] %s", source != null ? source : "<unknown>", message);
    }

    public static LogMessage info(Object aSource, String aFormat, Object... aValues)
    {
        return new LogMessage(aSource, LogLevel.INFO, aFormat, aValues);
    }

    public static LogMessage warn(Object aSource, String aFormat, Object... aValues)
    {
        return new LogMessage(aSource, LogLevel.WARN, aFormat, aValues);
    }

    public static LogMessage error(Object aSource, String aFormat, Object... aValues)
    {
        return new LogMessage(aSource, LogLevel.ERROR, aFormat, aValues);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof LogMessage)) {
            return false;
        }
        LogMessage castOther = (LogMessage) other;
        return Objects.equals(level, castOther.level) && Objects.equals(source, castOther.source)
                && Objects.equals(message, castOther.message);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, source, message);
    }
}
