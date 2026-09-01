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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * End-to-end check that the {@code remoteAddress} context variable documented in the admin guide
 * really shows up in the formatted log output when {@code logging.request.remote-address} is
 * enabled -- and that it does not when the setting is left at its default.
 */
class LoggingFilterRemoteAddressTest
{
    // The pattern the admin guide tells administrators to use. In an XML configuration file the
    // lookups are written "$${ctx:...}" so that log4j re-evaluates them for every event instead of
    // once at configuration time; a PatternLayout built in Java takes the pattern verbatim, so a
    // single "$" is the equivalent here.
    private static final String PATTERN = "[%encode{${ctx:username:-SYSTEM}}{CRLF}] "
            + "[%encode{${ctx:remoteAddress:-}}{CRLF}] %encode{%msg}{CRLF}%n";

    private static final String REMOTE_ADDRESS = "203.0.113.42";

    private CapturingAppender appender;
    private LoggerContext loggerContext;
    private Level originalLevel;

    @BeforeEach
    void setup()
    {
        loggerContext = (LoggerContext) LogManager.getContext(false);
        appender = new CapturingAppender(PatternLayout.newBuilder().withPattern(PATTERN)
                .withConfiguration(loggerContext.getConfiguration()).build());
        appender.start();
        // The shared log4j2-test.xml pins the root logger to WARN, which would swallow the INFO
        // messages this test logs, so raise the level for the duration of the test.
        originalLevel = loggerContext.getConfiguration().getRootLogger().getLevel();
        loggerContext.getConfiguration().getRootLogger().setLevel(Level.INFO);
        loggerContext.getConfiguration().getRootLogger().addAppender(appender, null, null);
        loggerContext.updateLoggers();
    }

    @AfterEach
    void teardown()
    {
        loggerContext.getConfiguration().getRootLogger().removeAppender(appender.getName());
        loggerContext.getConfiguration().getRootLogger().setLevel(originalLevel);
        loggerContext.updateLoggers();
        appender.stop();
    }

    @Test
    void thatRemoteAddressAppearsInLogOutputWhenEnabled() throws Exception
    {
        runRequestThroughFilter(true);

        assertThat(appender.messages) //
                .as("the client address ends up in the formatted log line") //
                .anyMatch(line -> line.contains("[" + REMOTE_ADDRESS + "]"));
    }

    @Test
    void thatRemoteAddressIsAbsentFromLogOutputByDefault() throws Exception
    {
        runRequestThroughFilter(false);

        assertThat(appender.messages) //
                .as("nothing is logged in place of the client address") //
                .isNotEmpty() //
                .noneMatch(line -> line.contains(REMOTE_ADDRESS)) //
                .allMatch(line -> line.contains("[] "));
    }

    @Test
    void thatRemoteAddressDoesNotLeakAfterTheRequestCompletes() throws Exception
    {
        runRequestThroughFilter(true);

        // Log from outside the filter -- e.g. a background thread reusing this thread later.
        LoggerFactory.getLogger(getClass()).info("after the request");

        assertThat(appender.messages) //
                .as("the MDC is cleaned up so later messages carry no stale address") //
                .filteredOn(line -> line.contains("after the request")) //
                .isNotEmpty() //
                .noneMatch(line -> line.contains(REMOTE_ADDRESS));
    }

    private void runRequestThroughFilter(boolean aEnabled) throws Exception
    {
        var request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);

        LoggingProperties properties = () -> aEnabled;
        var sut = new LoggingFilter(null, properties);

        // The chain stands in for the application code that logs while handling the request.
        sut.doFilter(request, null,
                (req, resp) -> LoggerFactory.getLogger(getClass()).info("handling request"));
    }

    private static final class CapturingAppender
        extends AbstractAppender
    {
        private final List<String> messages = new ArrayList<>();

        CapturingAppender(PatternLayout aLayout)
        {
            super("capture", null, aLayout, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(org.apache.logging.log4j.core.LogEvent aEvent)
        {
            messages.add(new String(getLayout().toByteArray(aEvent),
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
