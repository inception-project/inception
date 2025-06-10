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

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Arrays.asList;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.clipboardjs.ClipboardJsBehavior;
import org.wicketstuff.kendo.ui.form.datetime.DatePicker;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceProperties;
import de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class InviteProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 947691448582391801L;

    private @SpringBean InviteService inviteService;
    private @SpringBean InviteServiceProperties inviteServiceProperties;

    private IModel<ProjectInvite> invite;

    public InviteProjectSettingsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        invite = CompoundPropertyModel.of(inviteService.readProjectInvite(getModelObject()));

        add(new LambdaAjaxLink("regen", this::actionShareProject)
                .add(visibleWhenNot(invite.isPresent())));

        Form<ProjectInvite> detailsForm = new Form<>("form", invite);
        detailsForm.add(visibleWhen(invite.isPresent()));
        detailsForm.setOutputMarkupId(true);

        TextField<String> linkField = new TextField<>("linkText",
                invite.map(inviteService::getFullInviteLinkUrl))
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

        detailsForm.add(new TextArea<>("invitationText").add(AttributeModifier
                .replace("placeholder", new ResourceModel("invitationText.placeholder"))));

        detailsForm.add(new CheckBox("disableOnAnnotationComplete").setOutputMarkupId(true));

        detailsForm.add(new NumberTextField<Integer>("maxAnnotatorCount") //
                .setMinimum(0).setOutputMarkupId(true));

        detailsForm.add(new CheckBox("guestAccessible").setOutputMarkupId(true)
                .add(visibleWhen(() -> inviteServiceProperties.isGuestsEnabled()))
                .add(new LambdaAjaxFormSubmittingBehavior(CHANGE_EVENT, _t -> _t.add(this))));

        DropDownChoice<Mandatoriness> askForEMail = new DropDownChoice<>("askForEMail",
                asList(Mandatoriness.values()), new EnumChoiceRenderer<>(this));
        askForEMail.setOutputMarkupId(true);
        askForEMail.add(visibleWhen(() -> inviteServiceProperties.isGuestsEnabled()
                && invite.map(ProjectInvite::isGuestAccessible).orElse(false).getObject()));
        askForEMail.add(new LambdaAjaxFormSubmittingBehavior(CHANGE_EVENT, _t -> _t.add(this)));
        detailsForm.add(askForEMail);

        detailsForm.add(new TextField<>("userIdPlaceholder")
                .add(visibleWhen(invite.map(ProjectInvite::isGuestAccessible))));

        detailsForm.add(new WebMarkupContainer("alertExpirationDatePassed")
                .add(visibleWhen(invite.map(inviteService::isDateExpired))));
        detailsForm.add(new WebMarkupContainer("alertMaxAnnotatorsReached")
                .add(visibleWhen(invite.map(inviteService::isMaxAnnotatorCountReached))));
        detailsForm.add(new WebMarkupContainer("alertAnnotationFinished")
                .add(visibleWhen(invite.map(inviteService::isProjectAnnotationComplete))));

        detailsForm.add(new LambdaAjaxLink("extendLink", this::actionExtendInviteDate));
        detailsForm.add(new LambdaAjaxButton<>("save", this::actionSave));
        detailsForm.add(new LambdaAjaxLink("remove", this::actionRemoveInviteLink));

        add(detailsForm);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<ProjectInvite> aForm)
    {
        inviteService.writeProjectInvite(aForm.getModelObject());
        aTarget.add(this);

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
}
