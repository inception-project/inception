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

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.FailableRunnable;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class WebSocketSessionTestHandler
    extends StompSessionHandlerAdapter
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AtomicBoolean errorRecieved = new AtomicBoolean(false);

    private final FailableRunnable<Throwable> afterConnectedAction;
    private final String destination;
    private final Queue<ExpectedMessage> expectedMessages;

    private String errorMsg;

    private WebSocketSessionTestHandler(Builder builder)
    {
        afterConnectedAction = builder.afterConnectedAction;
        destination = builder.destination;
        expectedMessages = builder.expectedMessages;
    }

    @Override
    public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
    {
        if (destination != null) {
            aSession.subscribe(destination, new StompFrameHandlerImplementation());
        }

        // For some reason we have to wait a moment, otherwise the published event does not
        // go through
        Awaitility.with().pollDelay(100, MILLISECONDS).await().until(() -> true);

        if (afterConnectedAction != null) {
            try {
                afterConnectedAction.run();
            }
            catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void handleFrame(StompHeaders aHeaders, Object aPayload)
    {
        LOG.error("Error: {}", aHeaders.get("message"));
        errorMsg = aHeaders.getFirst("message");
        errorRecieved.set(true);
    }

    @Override
    public void handleException(StompSession aSession, StompCommand aCommand, StompHeaders aHeaders,
            byte[] aPayload, Throwable aException)
    {
        LOG.error("Exception: {}", aException.getMessage(), aException);
        errorMsg = aException.getMessage();
        errorRecieved.set(true);
    }

    @Override
    public void handleTransportError(StompSession aSession, Throwable aException)
    {
        LOG.error("Transport error: {}", aException.getMessage());
        // errorMsg = aException.getMessage();
        // errorRecieved.set(true);
        // responseRecievedLatch.countDown();
    }

    public boolean messagesProcessed()
    {
        return expectedMessages.isEmpty() || errorRecieved.get();
    }

    public void assertSuccess()
    {
        assertThat(errorMsg).isNull();
        assertThat(errorRecieved).isFalse();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public void assertError(Consumer<String> aErrorConsumer)
    {
        assertThat(errorRecieved).as("error was detected").isTrue();
        aErrorConsumer.accept(errorMsg);
    }

    private final class StompFrameHandlerImplementation
        implements StompFrameHandler
    {
        @Override
        public Type getPayloadType(StompHeaders aHeaders)
        {
            if (expectedMessages.isEmpty()) {
                return String.class;
            }

            return expectedMessages.peek().type;
        }

        @Override
        public void handleFrame(StompHeaders aHeaders, Object aPayload)
        {
            if (expectedMessages.isEmpty()) {
                Assertions.fail("Recieved unexpected message: {}", aPayload);
            }

            expectedMessages.peek().handler.accept(aHeaders, aPayload);
            expectedMessages.poll();
        }
    }

    private static final class ExpectedMessage
    {
        private final Type type;
        private final BiConsumer<StompHeaders, Object> handler;

        public ExpectedMessage(Type aType, BiConsumer<StompHeaders, Object> aHandler)
        {
            type = aType;
            handler = aHandler;
        }
    }

    public static final class Builder
    {
        private FailableRunnable<Throwable> afterConnectedAction;
        private String destination;
        private Queue<ExpectedMessage> expectedMessages = new LinkedList<>();

        private Builder()
        {
            // Construct only via factory method
        }

        @SuppressWarnings("unchecked")
        public <T> Builder expect(Class<T> aType, BiConsumer<StompHeaders, T> aCallback)
        {
            expectedMessages
                    .add(new ExpectedMessage(aType, (BiConsumer<StompHeaders, Object>) aCallback));
            return this;
        }

        public WebSocketSessionTestHandler build()
        {
            return new WebSocketSessionTestHandler(this);
        }
    }
}
