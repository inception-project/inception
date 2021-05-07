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
package de.tudarmstadt.ukp.inception.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.wicket.protocol.ws.util.tester.WebSocketTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;

@SpringBootTest
public class WebSocketTest
{

    private WicketTester tester;
    private WebSocketTestPage page;
    private TestEnv testVars;

    @BeforeEach
    public void setUp()
    {
        // set up "server" and test page
        tester = new WicketTester();
        testVars = new TestEnv();
        page = new WebSocketTestPage(testVars);
        tester.startPage(page);
    }

    @AfterEach
    public void tearDown()
    {
        tester.destroy();
    }

    @Test
    public void thatTextMessagesWork() throws Exception
    {
        String expectedMessage = "Hello from the WebSocket";
        testVars.setTestMessage(expectedMessage);

        WebSocketTester webSocketTester = new WebSocketTester(tester, page)
        {
            @Override
            protected void onOutMessage(String message)
            {
                // assert message equals
                assertThat(message).isEqualTo(expectedMessage);
            }
        };

        webSocketTester.sendMessage(expectedMessage);
        webSocketTester.destroy();
    }

    protected class TestEnv
    {

        private String testMessage;

        public String getTestMessage()
        {
            return testMessage;
        }

        public void setTestMessage(String testMessage)
        {
            this.testMessage = testMessage;
        }
    }

    @Configuration
    public static class TestContext
    {
        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }
    }
}
