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
package de.tudarmstadt.ukp.inception.experimental.api.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
/*
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "websocket.enabled=true"})
 */
public class ExperimentalAnnotationAPITests
{
    private WebSocketStompClient webSocketClient;
    private StompSession session;
    private String url = "ws://localhost:8080/inception_app_webapp_war_exploded/ws";

    //@BeforeEach
    public void setup() throws InterruptedException, ExecutionException, TimeoutException
    {
        webSocketClient = new WebSocketStompClient(new StandardWebSocketClient());
        webSocketClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSessionHandlerAdapter sessionHandler = new StompSessionHandlerAdapter()
        {

            @Override
            public void afterConnected(StompSession aSession,
                    StompHeaders aConnectedHeaders)
            {
                super.afterConnected(aSession, aConnectedHeaders);
            }
        };
        session = webSocketClient.connect(url, sessionHandler).get(5000, TimeUnit.MILLISECONDS);

    }

    //@AfterEach
    public void tearDown()
    {
        session.disconnect();
    }

    //@Test
    public void testNewDocument()
    {

    }

    //@Test
    public void testNewViewPort()
    {

    }


    //@Test
    public void testSelectAnnotation()
    {

    }

   //@Test
    public void testCreateAnnotation()
    {

    }

    //@Test
    public void testDeleteAnnotation()
    {

    }

}
