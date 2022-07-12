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
package de.tudarmstadt.ukp.clarin.webanno.support.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;

public class ChallengeResponseDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 5194857538069045172L;

    private IModel<String> titleModel;
    private IModel<String> challengeModel;
    private IModel<String> expectedResponseModel;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ChallengeResponseDialog(String aId)
    {
        this(aId, null, null, null);
    }

    public ChallengeResponseDialog(String aId, IModel<String> aTitle, IModel<String> aChallenge,
            IModel<String> aExpectedResponse)
    {
        super(aId);

        trapFocus();

        titleModel = aTitle;
        challengeModel = aChallenge;
        expectedResponseModel = aExpectedResponse;
    }

    public void show(AjaxRequestTarget aTarget)
    {
        var contentPanel = new ChallengeResponseDialogContentPanel(ModalDialog.CONTENT_ID);
        contentPanel.setConfirmAction(confirmAction);
        contentPanel.setCancelAction(cancelAction);
        contentPanel.setTitleModel(titleModel);
        contentPanel.setChallengeModel(challengeModel);
        contentPanel.setExpectedResponseModel(expectedResponseModel);
        contentPanel.onShow(aTarget);
        open(contentPanel, aTarget);
    }

    public IModel<String> getTitleModel()
    {
        return titleModel;
    }

    public void setTitleModel(IModel<String> aTitleModel)
    {
        titleModel = aTitleModel;
    }

    public IModel<String> getChallengeModel()
    {
        return challengeModel;
    }

    public void setChallengeModel(IModel<String> aChallengeModel)
    {
        challengeModel = aChallengeModel;
    }

    public IModel<String> getResponseModel()
    {
        return expectedResponseModel;
    }

    public void setResponseModel(IModel<String> aExpectedResponseModel)
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
