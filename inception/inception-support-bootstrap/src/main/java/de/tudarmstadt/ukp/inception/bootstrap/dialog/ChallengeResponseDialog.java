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
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;

/**
 * @deprecated Use {@link BootstrapModalDialog#open(org.apache.wicket.Component, AjaxRequestTarget)}
 *             to directly open a dialog content panel.
 */
@Deprecated
public class ChallengeResponseDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 5194857538069045172L;

    private ResourceModel titleModel;
    private ResourceModel messageModel;
    private IModel<String> expectedResponseModel;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ChallengeResponseDialog(String aId)
    {
        super(aId);
        trapFocus();
    }

    public void show(AjaxRequestTarget aTarget)
    {
        var contentPanel = new ChallengeResponseDialogContentPanel(ModalDialog.CONTENT_ID);
        contentPanel.setConfirmAction(confirmAction);
        contentPanel.setCancelAction(cancelAction);
        contentPanel.setTitleModel(titleModel);
        contentPanel.setMessageModel(messageModel);
        contentPanel.setExpectedResponseModel(expectedResponseModel);
        contentPanel.onShow(aTarget);
        open(contentPanel, aTarget);
    }

    public void setTitleModel(ResourceModel aTitleModel)
    {
        titleModel = aTitleModel;
    }

    public void setMessageModel(ResourceModel aMessageModel)
    {
        messageModel = aMessageModel;
    }

    public IModel<String> getExpectedResponseModel()
    {
        return expectedResponseModel;
    }

    public void setExpectedResponseModel(IModel<String> aExpectedResponseModel)
    {
        expectedResponseModel = aExpectedResponseModel;
    }

    public AjaxCallback getConfirmAction()
    {
        return confirmAction;
    }

    public void setConfirmAction(AjaxCallback aConfirmAction)
    {
        confirmAction = aConfirmAction;
    }

    public AjaxCallback getCancelAction()
    {
        return cancelAction;
    }

    public void setCancelAction(AjaxCallback aCancelAction)
    {
        cancelAction = aCancelAction;
    }
}
