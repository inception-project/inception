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
package de.tudarmstadt.ukp.inception.bootstrap;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * {@code FeedbackPanel} which applies Bootstrap alert styles to feedback messages.
 */
public class BootstrapFeedbackPanel
    extends FeedbackPanel
{
    private static final long serialVersionUID = 5171764027460264375L;

    private WebMarkupContainer closeAll;

    public BootstrapFeedbackPanel(String id)
    {
        this(id, null);
    }

    public BootstrapFeedbackPanel(String id, IFeedbackMessageFilter filter)
    {
        super(id, filter);

        WebMarkupContainer messagesContainer = (WebMarkupContainer) get("feedbackul");

        closeAll = new WebMarkupContainer("closeAll");
        closeAll.setOutputMarkupId(true);
        // Show the bulk-dismiss option if there are at least two sticky messages t
        closeAll.add(visibleWhen(() -> getCurrentMessages().stream()
                .filter(FeedbackMessage::isWarning).limit(2).count() > 1));

        messagesContainer.add(closeAll);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(
                JavaScriptHeaderItem.forReference(BootstrapFeedbackPanelJavascriptReference.get()));

        aResponse.render(OnDomReadyHeaderItem.forScript("bootstrapFeedbackPanelFade();"));

        if (closeAll.isVisible()) {
            aResponse.render(OnDomReadyHeaderItem.forScript("document.getElementById('"
                    + closeAll.getMarkupId()
                    + "')?.addEventListener('click', e => bootstrapFeedbackPanelCloseAll())"));
        }
    }

    @Override
    protected String getCSSClass(FeedbackMessage message)
    {
        String cssClass = "alert alert-dismissible";
        switch (message.getLevel()) {
        case FeedbackMessage.ERROR:
        case FeedbackMessage.FATAL:
            cssClass += " alert-danger";
            break;
        case FeedbackMessage.SUCCESS:
            cssClass += " alert-success";
            break;
        case FeedbackMessage.WARNING:
            cssClass += " alert-warning";
            break;
        case FeedbackMessage.INFO:
            cssClass += " alert-info";
            break;
        default:
            break;
        }
        return cssClass;
    }
}
