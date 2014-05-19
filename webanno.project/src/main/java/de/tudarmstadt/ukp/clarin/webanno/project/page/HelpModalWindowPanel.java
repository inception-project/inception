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
package de.tudarmstadt.ukp.clarin.webanno.project.page;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;

/**
 * A panel that is used to display a {@link TextField} or {@link Label} to add/display help contents
 *
 * @author Seid Muhie Yimam
 *
 */
public class HelpModalWindowPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private class HelpDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public HelpDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(new MultiLineLabel("helpContent", helpDataModel).setEscapeModelStrings(false));
          /*  add(new TextArea<String>("helpContent", helpDataModel)
                    .setOutputMarkupPlaceholderTag(true));*/
            add(new AjaxButton("close")
            {
                private static final long serialVersionUID = 8922161039500097566L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aModalWindow.close(aTarget);
                }
            });
            HelpDialogForm.this.setMultiPart(true);
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = 4827604877563167791L;
        public String helpContent;
    }

    private String helpField;
    private HelpDataModel helpButton;
    private final HelpDialogForm helpDialogForm;
    private IModel<String> helpDataModel;

    public HelpModalWindowPanel(String aId, final ModalWindow aModalWindow,
            HelpDataModel aHelpButton, String aHelpFiel, IModel<String> aHelpDataModel)
    {
        super(aId);

        helpField = aHelpFiel;
        helpDataModel = aHelpDataModel;
        helpButton = aHelpButton;
        helpDialogForm = new HelpDialogForm("helpDialogForm", aModalWindow);
        add(helpDialogForm);

    }
}
