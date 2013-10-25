/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class GuidelineModalWindowPage
    extends WebPage
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private class guidelineForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public guidelineForm(String id, final ModalWindow modalWindow)
        {
            super(id);


            // Overall progress by Projects
            RepeatingView guidelineRepeater = new RepeatingView("guidelineRepeater");
            add(guidelineRepeater);

            for (String guidelineFileName : repository.listGuidelines(project)) {
                AbstractItem item = new AbstractItem(guidelineRepeater.newChildId());

                guidelineRepeater.add(item);

             // Add a popup window link to display annotation guidelines
                PopupSettings popupSettings = new PopupSettings(PopupSettings.RESIZABLE
                        | PopupSettings.SCROLLBARS).setHeight(500).setWidth(700);

                IResourceStream stream = new FileResourceStream(repository.getGuideline(project, guidelineFileName));
                ResourceStreamResource resource = new ResourceStreamResource(stream);
                ResourceLink<Void> rlink = new ResourceLink<Void>("guideine", resource);
                rlink.setPopupSettings(popupSettings);
                item.add(new Label("guidelineName", guidelineFileName));
                item.add(rlink);

            }

            add(new AjaxLink<Void>("close")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    modalWindow.close(target);
                }
            });
        }
    }

    private guidelineForm guidelineForm;
    private Project project;

    public GuidelineModalWindowPage(final ModalWindow modalWindow, Project aProject)
    {
        this.project = aProject;
        guidelineForm = new guidelineForm("guidelineForm", modalWindow);
        add(guidelineForm);
    }

}
