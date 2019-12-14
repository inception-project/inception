/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * {@code FeedbackPanel} which applies Bootstrap alert styles to feedback messages.
 */
public class BootstrapFeedbackPanel
    extends FeedbackPanel
{
    private static final long serialVersionUID = 5171764027460264375L;

    public BootstrapFeedbackPanel(String id)
    {
        this(id, null);
    }

    public BootstrapFeedbackPanel(String id, IFeedbackMessageFilter filter)
    {
        super(id, filter);

        WebMarkupContainer messagesContainer = (WebMarkupContainer) get("feedbackul");

        WebMarkupContainer closeAll = new WebMarkupContainer("closeAll");
        closeAll.add(visibleWhen(
            () -> getCurrentMessages().stream().anyMatch(FeedbackMessage::isWarning)));
        messagesContainer.add(closeAll);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message)
    {
        String cssClass = "alert alert-dismissable";
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
