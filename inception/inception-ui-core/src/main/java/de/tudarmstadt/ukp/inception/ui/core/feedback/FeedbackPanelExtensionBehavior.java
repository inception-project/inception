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
package de.tudarmstadt.ukp.inception.ui.core.feedback;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFeedbackPanel;

public class FeedbackPanelExtensionBehavior
    extends Behavior
{
    private static final long serialVersionUID = 1L;
    private final static Logger LOG = LoggerFactory.getLogger(FeedbackPanelExtensionBehavior.class);

    public String retrieveFeedbackPanelId(Component aComponent)
    {
        Page page = null;
        BootstrapFeedbackPanel feedbackPanel = null;
        String feedbackPanelId = "";
        try {
            page = aComponent.getPage();
        }
        catch (WicketRuntimeException e) {
            LOG.debug("No page yet.");
        }
        if (page != null) {
            feedbackPanel = aComponent.getPage().visitChildren(BootstrapFeedbackPanel.class,
                    new IVisitor<BootstrapFeedbackPanel, BootstrapFeedbackPanel>()
                    {

                        @Override
                        public void component(BootstrapFeedbackPanel aFeedbackPanel,
                                IVisit<BootstrapFeedbackPanel> aVisit)
                        {
                            aVisit.stop(aFeedbackPanel);
                        }
                    });
            if (feedbackPanel != null) {
                feedbackPanelId = feedbackPanel.getMarkupId();
            }
        }
        return feedbackPanelId;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);
        aResponse.render(
                JavaScriptHeaderItem.forReference(FeedbackPanelExtensionJavascriptReference.get()));
    }

}
