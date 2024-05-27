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
package de.tudarmstadt.ukp.inception.support;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Stop watch class that can be used as a resource in try-catch blocks and logs the time used when
 * the block completes.
 */
public class StopWatch
    implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(StopWatch.class);

    private Level level;
    private Logger log;
    private String message;
    private Object[] values;
    private long startTime;
    private long stopTime;
    private boolean running = false;

    public StopWatch(String aMessage, Object... aValues)
    {
        this(LOG, aMessage, aValues);
    }

    public StopWatch(Logger aLogger, String aMessage, Object... aValues)
    {
        this(aLogger, Level.TRACE, aMessage, aValues);
    }

    public StopWatch(Logger aLogger, Level aLevel, String aMessage, Object... aValues)
    {
        message = aMessage;
        log = aLogger;
        level = aLevel;
        values = aValues;

        start();
    }

    private String getMessage()
    {
        long duration = System.currentTimeMillis() - startTime;

        return format("[%4dms] %s", duration, format(message, values));
    }

    public long getTime()
    {
        if (running) {
            return System.currentTimeMillis() - startTime;
        }
        else {
            return stopTime;
        }
    }

    public void start()
    {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public long stop()
    {
        long stop = getTime();
        running = false;
        return stop;
    }

    @Override
    public void close()
    {
        switch (level) {
        case DEBUG:
            if (log.isDebugEnabled()) {
                log.debug(getMessage());
            }
            break;
        case ERROR:
            if (log.isErrorEnabled()) {
                log.error(getMessage());
            }
            break;
        case INFO:
            if (log.isInfoEnabled()) {
                log.info(getMessage());
            }
            break;
        case TRACE:
            if (log.isTraceEnabled()) {
                log.trace(getMessage());
            }
            break;
        case WARN:
            if (log.isWarnEnabled()) {
                log.warn(getMessage());
            }
            break;
        }
    }
}
