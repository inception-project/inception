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

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * {@code FeedbackPanel} which applies Bootstrap alert styles to feedback messages.
 */
public class BootstrapFeedbackPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5171764027460264375L;

    public BootstrapFeedbackPanel(String id) {
        super(id);
        
        initCloseAll();
    }

    public BootstrapFeedbackPanel(String id, IFeedbackMessageFilter filter) {
        super(id, filter);
        
        initCloseAll();
    }
    
    private void initCloseAll()
    {
        WebMarkupContainer messagesContainer = (WebMarkupContainer) get("feedbackul");
        
        WebMarkupContainer closeAll = new WebMarkupContainer("closeAll") {
            private static final long serialVersionUID = -2488179250168075146L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                // If there is more than 1 sticky messages, then show the close-all button
                int stickyMessages = 0;
                for (FeedbackMessage msg : getCurrentMessages()) {
                    if ((!msg.isSuccess() || msg.isInfo())) {
                        stickyMessages ++;
                    }
                    if (stickyMessages > 1) {
                        break;
                    }
                }
                
                setVisible(stickyMessages > 1);
            }
        };
        
        messagesContainer.add(closeAll);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
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
