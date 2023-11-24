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
package de.tudarmstadt.ukp.inception.bootstrap.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

/**
 * @deprecated Subclass {@link ChallengeResponseDialogContentPanel_ImplBase} and create a custom
 *             panel for your dialog content.
 */
@Deprecated
public class ChallengeResponseDialogContentPanel
    extends ChallengeResponseDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = 5202661827792148838L;

    private Label message;
    private ResourceModel messageModel;

    public ChallengeResponseDialogContentPanel(String aId)
    {
        super(aId);

        message = new Label("message", messageModel);
        message.setEscapeModelStrings(false); // SAFE - We use non-parametrized a ResourceModel here
                                              // which can only come from a trusted resource bundle

        queue(message);
    }

    @Deprecated
    @Override
    public void onShow(AjaxRequestTarget aTarget)
    {
        message.setDefaultModel(messageModel);
        super.onShow(aTarget);
    }

    public void setMessageModel(ResourceModel aMessageModel)
    {
        messageModel = aMessageModel;
    }
}
