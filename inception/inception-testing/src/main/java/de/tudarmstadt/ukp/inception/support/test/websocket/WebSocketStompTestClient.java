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
package de.tudarmstadt.ukp.inception.support.test.websocket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableRunnable;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class WebSocketStompTestClient
    implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Duration connectTimeout = Duration.ofSeconds(60);
    private final Duration messageTimeout = Duration.ofSeconds(10);

    private final String username;
    private final String password;
    private final StandardWebSocketClient wsClient;
    private final WebSocketStompClient stompClient;
    private final MessageConverter messageConverter;

    private CapturingSessionHandler sessionHandler;
    private StompSession session;

    private Queue<Object> recieved;
    private Queue<Expectation<?>> expectations;

    public WebSocketStompTestClient(String aUsername, String aPassword)
    {
        username = aUsername;
        password = aPassword;

        recieved = new ConcurrentLinkedQueue<>();
        expectations = new ConcurrentLinkedDeque<>();
        messageConverter = new MappingJackson2MessageConverter();

        wsClient = new StandardWebSocketClient();
        wsClient.setUserProperties(Map.of( //
                WS_AUTHENTICATION_USER_NAME, username, //
                WS_AUTHENTICATION_PASSWORD, password));

        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new Message2MessageConverter());
    }

    public StompSession connect(String aUrl)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        assertThat(expectations).as("expectations").isNotEmpty();
        assertThat(session).as("already connected").isNull();

        sessionHandler = new CapturingSessionHandler(recieved);
        session = stompClient //
                .connectAsync(aUrl, getHeaders(), sessionHandler) //
                .get(connectTimeout.getSeconds(), SECONDS);

        await("all expected responses recieved") //
                .atMost(connectTimeout) //
                .until(() -> recieved.size() >= expectations.size());

        await("extra time") //
                .pollDelay(Duration.ofSeconds(1)) //
                .until(() -> true);

        handleExpectations();

        LOG.trace("Session established");

        return session;
    }

    public WebSocketStompTestClient expectSuccessfulConnection()
    {
        expectations.add(new Expectation<>(ConnectionResponse.class, false, response -> {
            assertThat(response.headers().get("user-name")).containsExactly(username);
        }));
        return this;
    }

    public WebSocketStompTestClient expectError(String aErrorMessage)
    {
        expectations.add(new Expectation<>(NoPayloadResponse.class, true, response -> {
            assertThat(response.headers().get("message")).containsExactly(aErrorMessage);
        }));
        return this;
    }

    public WebSocketStompTestClient expect(Object aObject)
    {
        expectations.add(new Expectation<>(Message.class, false, msg -> {
            try {
                var obj = messageConverter.fromMessage(msg, aObject.getClass());
                assertThat(obj).isEqualTo(aObject);
            }
            catch (MessageConversionException e) {
                fail("Unable to convert message: " + msg + " with payload "
                        + new String((byte[]) msg.getPayload(), UTF_8), e);
            }
        }));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> WebSocketStompTestClient expect(T aObject, FailableConsumer<T, Exception> aMatcher)
    {
        expectations.add(new Expectation<>(Message.class, false, msg -> {
            try {
                var obj = messageConverter.fromMessage(msg, aObject.getClass());
                aMatcher.accept((T) obj);
            }
            catch (MessageConversionException e) {
                fail("Unable to convert message: " + msg + " with payload "
                        + new String((byte[]) msg.getPayload(), UTF_8), e);
            }
        }));
        return this;
    }

    /**
     * Subscribes to a topic. Requires expected messages to be queued before.
     * 
     * @param aTopic
     *            topic to subscribe to
     * @return subscription.
     */
    public Subscription subscribe(String aTopic)
    {
        var handler = new CapturingFrameHandler<>(Message.class, recieved);
        var sub = session.subscribe(aTopic, handler);

        waitForMessages();

        handleExpectations();

        LOG.trace("Subscription to [{}] successful", aTopic);

        return sub;
    }

    public void perform(FailableRunnable<Exception> aTask) throws Exception
    {
        aTask.run();

        waitForMessages();

        handleExpectations();
    }

    public Receiptable send(String aTopic, Object aPayload)
    {
        return session.send(aTopic, aPayload);
    }

    public void assertExpectations()
    {
        waitForMessages();

        handleExpectations();
    }

    private void waitForMessages()
    {
        await("all expected messages recieved") //
                .atMost(messageTimeout) //
                .until(() -> recieved.size() >= expectations.size());

        await("extra time") //
                .pollDelay(Duration.ofSeconds(1)) //
                .until(() -> true);
    }

    private void handleExpectations()
    {
        var r = new LinkedList<>(recieved);
        var e = new LinkedList<>(expectations);

        expectations.clear();
        recieved.clear();

        var consuming = false;
        while (!r.isEmpty() && !e.isEmpty()) {
            LOG.trace("Remaining: {} messages  {} expectation", r.size(), e.size());

            var message = r.poll();
            var expectation = e.poll();

            consuming = expectation.isConsuming();
            expectation.accept(message);
        }

        if (!consuming) {
            LOG.trace("Remaining: {} messages  {} expectation", r.size(), e.size());
            assertThat(r).as("remaining recieved messages").isEmpty();
            assertThat(e).as("remaining expectations messages").isEmpty();
        }
        else {
            LOG.trace("Consumed remaining: {} messages  {} expectation", r.size(), e.size());
        }

        // For some reason we have to wait a moment, otherwise the published event does not
        // go through
        Awaitility.with().pollDelay(100, MILLISECONDS).await().until(() -> true);
    }

    private WebSocketHttpHeaders getHeaders()
    {
        var headers = new WebSocketHttpHeaders();
        if (username != null && password != null) {
            headers.add("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        }
        return headers;
    }

    @Override
    public void close() throws IOException
    {
        if (session != null) {
            try {
                session.disconnect();
            }
            catch (Exception e) {
                // Ignore exceptions during disconnect
            }
            finally {
                session = null;
            }
            LOG.trace("Session disconnected");
        }
    }

    private final class Message2MessageConverter
        extends MappingJackson2MessageConverter
    {
        @Override
        protected Object convertFromInternal(Message<?> aMessage, Class<?> aTargetClass,
                Object aConversionHint)
        {
            return aMessage;
        }
    }

    private interface Response
    {
    }

    public record ConnectionResponse(StompHeaders headers, Object payload)
        implements Response
    {}

    public record NoPayloadResponse(StompHeaders headers, Object payload)
        implements Response
    {}

    public record ExceptionResponse(StompSession aSession, StompCommand aCommand,
            StompHeaders aHeaders, byte[] aPayload, Throwable aException)
        implements Response
    {};

    public record TransportErrorResponse(StompSession aSession, Throwable aException)
        implements Response
    {};

    private static class CapturingSessionHandler
        extends StompSessionHandlerAdapter
    {
        private final Queue<Object> recieved;

        CapturingSessionHandler(Queue<Object> aQueue)
        {
            recieved = aQueue;
        }

        @Override
        public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
        {
            LOG.trace("Recieved: connection: {}", aConnectedHeaders);
            recieved.add(new ConnectionResponse(aConnectedHeaders, null));
        }

        @Override
        public void handleFrame(StompHeaders aHeaders, Object aPayload)
        {
            LOG.trace("Recieved: frame: {}", aPayload);
            recieved.add(new NoPayloadResponse(aHeaders, aPayload));
        }

        @Override
        public void handleException(StompSession aSession, StompCommand aCommand,
                StompHeaders aHeaders, byte[] aPayload, Throwable aException)
        {
            LOG.trace("Recieved: exception", aException);
            recieved.add(new ExceptionResponse(aSession, aCommand, aHeaders, aPayload, aException));
        }

        @Override
        public void handleTransportError(StompSession aSession, Throwable aException)
        {
            LOG.trace("Recieved: transport error", aException);
            recieved.add(new TransportErrorResponse(aSession, aException));
        }
    }

    private static class Expectation<T>
    {
        private final Class<T> type;
        private final FailableConsumer<T, Exception> action;
        private final boolean consuming;

        public Expectation(Class<T> aType, boolean aConsuming,
                FailableConsumer<T, Exception> aAction)
        {
            type = aType;
            action = aAction;
            consuming = aConsuming;
        }

        public Class<T> getType()
        {
            return type;
        }

        public void accept(Object aObject)
        {
            try {
                action.accept((T) aObject);
            }
            catch (Exception e) {
                Assertions.fail(e);
            }
        }

        public boolean isConsuming()
        {
            return consuming;
        }
    }

    private static class CapturingFrameHandler<T>
        implements StompFrameHandler
    {
        private static final Logger LOG = LoggerFactory
                .getLogger(MethodHandles.lookup().lookupClass());

        private final Class<T> type;
        private final Queue<Object> recieved;

        CapturingFrameHandler(Class<T> aType, Queue<Object> aQueue)
        {
            type = aType;
            recieved = aQueue;
        }

        @Override
        public Type getPayloadType(StompHeaders aHeaders)
        {
            return type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleFrame(StompHeaders aHeaders, Object aPayload)
        {
            if (type.isAssignableFrom(aPayload.getClass())) {
                LOG.trace("Recieved: {}", aPayload);
                recieved.add((T) aPayload);
                return;
            }

            LOG.error("Expected message of type [{}] but got: [{}]", type.getName(), aPayload);
        }
    }
}
