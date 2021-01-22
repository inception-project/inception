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
package de.tudarmstadt.ukp.inception.sharing.panel;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.clipboardjs.ClipboardJsBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.sharing.InviteService;

public class InviteLinkPanel
    extends Panel
{
    private static final long serialVersionUID = 947691448582391801L;
    
    public static final String PAGE_PARAM_INVITE_ID = "i";

    private @SpringBean InviteService inviteService;

    private final WebMarkupContainer inviteLinkContainer;
    private final WebMarkupContainer linkMainContainer;
    private TextField<String> linkField;
    private IModel<Class<? extends Page>> baseUrlClass;

    public InviteLinkPanel(String aId, IModel<Project> aProjectModel, IModel<Class<? extends Page>> aBaseUrlClass)
    {
        super(aId, aProjectModel);
        baseUrlClass = aBaseUrlClass;

        linkMainContainer = new WebMarkupContainer("linkMainContainer");
        linkMainContainer.setOutputMarkupId(true);
        // FIXME adding this line: only annotators can no longer click on annotation to access the
        // annopage
//        linkMainContainer.add(visibleWhen(() -> isManager));
        add(linkMainContainer);

        inviteLinkContainer = createInviteLinkContainer();
        inviteLinkContainer.add(visibleWhen(() -> linkField.getModelObject() != null));
        inviteLinkContainer.setOutputMarkupId(true);
        linkMainContainer.add(inviteLinkContainer);

        AjaxLink<Void> shareBtn = new AjaxLink<>("shareProject")
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                shareProject();
                aTarget.add(linkMainContainer);
            }

        };
        shareBtn.add(visibleWhen(() -> !inviteLinkContainer.isVisible()));
        linkMainContainer.add(shareBtn);
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
        AjaxLink<Void> regenBtn = new AjaxLink<>("regen")
        {

            private static final long serialVersionUID = 8558630925669881073L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                shareProject();
                aTarget.add(linkContainer);
            }

        };
        linkContainer.add(regenBtn);;
        AjaxLink<Void> removeBtn = new AjaxLink<>("remove")
        {
            private static final long serialVersionUID = 4847153359605500314L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                removeInviteLink();
                aTarget.add(linkMainContainer);
            }

        };
        linkContainer.add(removeBtn);
        return linkContainer;
    }

    private void shareProject()
    {
        inviteService.generateInviteID(((Project) getDefaultModel().getObject()));
    }

    private void removeInviteLink()
    {
        inviteService.removeInviteID(((Project) getDefaultModel().getObject()));
    }

    private String getInviteLink()
    {
        Long projectId = ((Project) getDefaultModel().getObject()).getId();
        String inviteId = inviteService.getValidInviteID(projectId);

        if (inviteId == null) {
            return null;
        }

        PageParameters pageParams = new PageParameters();
        pageParams.add(PAGE_PARAM_INVITE_ID, inviteId);
        pageParams.add(PAGE_PARAM_PROJECT_ID, projectId);
        String fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(
                Url.parse(RequestCycle.get().urlFor(baseUrlClass.getObject(), pageParams)));
        return fullUrl;
    }

}
