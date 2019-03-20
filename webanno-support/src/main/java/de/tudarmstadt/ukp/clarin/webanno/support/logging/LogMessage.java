/*
 * Copyright 2018
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
 */
package de.tudarmstadt.ukp.clarin.webanno.support.logging;

import java.io.Serializable;

public class LogMessage
    implements Serializable
{
    private static final long serialVersionUID = 2002139781814027105L;

    public final LogLevel level;
    public final Class<?> source;
    public final String message;

    public LogMessage(Object aSource, LogLevel aLevel, String aMessage)
    {
        this(aSource, aLevel, "%s", aMessage);
    }

    public LogMessage(Object aSource, LogLevel aLevel, String aFormat, Object... aValues)
    {
        super();
        if (aSource instanceof Class) {
            source = (Class) aSource;
        }
        else {
            source = aSource != null ? aSource.getClass() : null;
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
    
    public Class<?> getSource()
    {
        return source;
    }
    
    @Override
    public String toString()
    {
        return String.format("[%s] %s", source != null ? source.getSimpleName() : "<unknown>",
                message);
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
}
