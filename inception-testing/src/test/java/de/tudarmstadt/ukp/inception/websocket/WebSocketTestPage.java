/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

import de.tudarmstadt.ukp.inception.websocket.WebSocketTest.TestEnv;

public class WebSocketTestPage
    extends WebPage implements IMarkupResourceStreamProvider
{

    private static final long serialVersionUID = 7883353311808962940L;
    private TestEnv testVars;

    public WebSocketTestPage(TestEnv aTestVars)
    {
        testVars = aTestVars;

        // add WebSocketBehaviour to listen to messages
        add(new WebSocketBehavior()
        {

            private static final long serialVersionUID = -4076782377506950851L;

            @Override
            protected void onPush(WebSocketRequestHandler aHandler, IWebSocketPushMessage aMessage)
            {
                assertThat(aMessage).isEqualTo(testVars.getTestMessage());
                super.onPush(aHandler, aMessage);
            }
        });
    }

    @Override
    public IResourceStream getMarkupResourceStream(MarkupContainer aContainer,
            Class<?> aContainerClass)
    {
        return new StringResourceStream("<html><body></body></html>");
    }
}
