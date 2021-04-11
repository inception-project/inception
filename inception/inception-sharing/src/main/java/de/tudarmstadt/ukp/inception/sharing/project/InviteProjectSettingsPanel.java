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
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhenNot;

import javax.servlet.ServletContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.clipboardjs.ClipboardJsBehavior;

import com.googlecode.wicket.kendo.ui.form.datetime.DatePicker;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.sharing.AcceptInvitePage;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

public class InviteProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691448582391801L;

    private @SpringBean InviteService inviteService;
    private @SpringBean ServletContext servletContext;

    private IModel<ProjectInvite> invite;

    public InviteProjectSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        invite = CompoundPropertyModel.of(inviteService.readProjectInvite(getModelObject()));

        add(new LambdaAjaxLink("regen", this::actionShareProject)
                .add(visibleWhenNot(invite.isPresent())));

        Form<ProjectInvite> detailsForm = new Form<>("form", invite);
        TextField<String> linkField = new TextField<>("linkText",
                LoadableDetachableModel.of(this::getInviteLink))
        {
            private static final long serialVersionUID = -4045558203619280212L;

            protected void onDisabled(ComponentTag tag)
            {
                // Override the default "disabled" because ClipboardJsBehavior does not like it
                tag.put("readonly", "readonly");
            };
        };
        linkField.setEnabled(false);
        detailsForm.add(linkField);
        detailsForm.add(new WebMarkupContainer("copyLink")
                .add(new ClipboardJsBehavior().setTarget(linkField)));
        detailsForm.add(new DatePicker("expirationDate", "yyyy-MM-dd"));
        detailsForm.add(new TextArea<>("invitationText"));
        detailsForm.add(new CheckBox("guestAccessible").setOutputMarkupId(true));
        detailsForm.add(new LambdaAjaxLink("extendLink", this::actionExtendInviteDate));
        detailsForm.add(new LambdaAjaxButton<>("save", this::actionSave));
        detailsForm.add(new LambdaAjaxLink("remove", this::actionRemoveInviteLink));
        detailsForm.add(visibleWhen(invite.isPresent()));
        add(detailsForm);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<ProjectInvite> aForm)
    {
        inviteService.writeProjectInvite(aForm.getModelObject());
        success("Settings saved.");
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionExtendInviteDate(AjaxRequestTarget aTarget)
    {
        inviteService.extendInviteLinkDate(getModelObject());
        invite.setObject(inviteService.readProjectInvite(getModelObject()));

        aTarget.add(this);
        success("The validity period has been extended.");
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionShareProject(AjaxRequestTarget aTarget)
    {
        inviteService.generateInviteID(getModelObject());
        invite.setObject(inviteService.readProjectInvite(getModelObject()));
        aTarget.add(this);
    }

    private void actionRemoveInviteLink(AjaxRequestTarget aTarget)
    {
        inviteService.removeInviteID(getModelObject());
        invite.setObject(null);
        aTarget.add(this);
    }

    private String getInviteLink()
    {
        String inviteId = inviteService.getValidInviteID(getModelObject());

        if (inviteId == null) {
            return null;
        }

        CharSequence url = urlFor(AcceptInvitePage.class,
                new PageParameters()
                        .set(AcceptInvitePage.PAGE_PARAM_PROJECT, getModelObject().getId())
                        .set(AcceptInvitePage.PAGE_PARAM_INVITE_ID, inviteId));

        return RequestCycle.get().getUrlRenderer().renderFullUrl(Url.parse(url));
    }
}
