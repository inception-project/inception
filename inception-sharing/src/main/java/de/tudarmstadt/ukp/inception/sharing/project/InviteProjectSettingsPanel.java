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
package de.tudarmstadt.ukp.inception.sharing.project;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.clipboardjs.ClipboardJsBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.sharing.InviteService;

public class InviteProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691448582391801L;

    public static final String PAGE_PARAM_INVITE_ID = "i";

    private @SpringBean InviteService inviteService;
    private @SpringBean ServletContext servletContext;

    private final WebMarkupContainer inviteLinkContainer;
    private TextField<String> linkField;

    public InviteProjectSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);

        inviteLinkContainer = createInviteLinkContainer();
        inviteLinkContainer.setOutputMarkupId(true);
        add(inviteLinkContainer);
        // add expiration infos
        Label expirationLabel = new Label("expirationDate", LoadableDetachableModel.of(this::getExpirationDate));
        expirationLabel.setOutputMarkupId(true);
        expirationLabel.add(visibleWhen(() -> linkField.getModelObject() != null));
        inviteLinkContainer.add(expirationLabel);
        LambdaAjaxLink extendBtn = new LambdaAjaxLink("extendLink", this::actionExtendInviteDate);
        inviteLinkContainer.add(extendBtn);
    }
    
    private void actionExtendInviteDate(AjaxRequestTarget aTarget) {
        aTarget.addChildren(getPage(), IFeedback.class);
        inviteService.extendInviteLinkDate(getModelObject());
        success("The validity period has been extended.");
        aTarget.add(inviteLinkContainer);
    }
    
    private String getExpirationDate() {
        Date expirationDate = inviteService.getExpirationDate(getModelObject());
        if (expirationDate == null)
        {
            return null;
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(expirationDate);
    }

    private WebMarkupContainer createInviteLinkContainer()
    {
        WebMarkupContainer linkContainer = new WebMarkupContainer("invitelinkContainer");
        linkContainer.setOutputMarkupId(true);

        linkField = new TextField<>("linkText", LoadableDetachableModel.of(this::getInviteLink));
        linkContainer.add(linkField);

        Button copyBtn = new Button("copy");
        ClipboardJsBehavior clipboardBehavior = new ClipboardJsBehavior();
        clipboardBehavior.setTarget(linkField);
        copyBtn.add(clipboardBehavior);
        linkContainer.add(copyBtn);
        LambdaAjaxLink regenBtn = new LambdaAjaxLink("regen", this::actionShareProject);
        linkContainer.add(regenBtn);
        LambdaAjaxLink removeBtn = new LambdaAjaxLink("remove", this::actionRemoveInviteLink);
        linkContainer.add(removeBtn);
        return linkContainer;
    }

    private void actionShareProject(AjaxRequestTarget aTarget)
    {
        inviteService.generateInviteID(getModelObject());
        aTarget.add(inviteLinkContainer);
    }

    private void actionRemoveInviteLink(AjaxRequestTarget aTarget)
    {
        inviteService.removeInviteID(getModelObject());
        aTarget.add(inviteLinkContainer);
    }

    private String getInviteLink()
    {
        String inviteId = inviteService.getValidInviteID(getModelObject());

        if (inviteId == null) {
            return null;
        }

        Url inviteUrl = Url.parse(String.format("%s/join-project/%s/%s",
                servletContext.getContextPath(), getModelObject().getId(), inviteId));
        String fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(inviteUrl);
        return fullUrl;
    }

}
