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
package de.tudarmstadt.ukp.inception.websocket.config.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class LoggingStompSessionHandlerAdapter
    extends StompSessionHandlerAdapter
{
    private final Logger log;

    public LoggingStompSessionHandlerAdapter()
    {
        log = LoggerFactory.getLogger(LoggingStompSessionHandlerAdapter.class);
    }

    public LoggingStompSessionHandlerAdapter(Logger aLog)
    {
        log = aLog;
    }

    @Override
    public void handleException(StompSession aSession, StompCommand aCommand, StompHeaders aHeaders,
            byte[] aPayload, Throwable aException)
    {
        log.error("Error handling STOMP message", aException);
    }

    @Override
    public void handleTransportError(StompSession aSession, Throwable aException)
    {
        log.error("STOMP transport error", aException);
    }
}
