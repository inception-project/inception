/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;

/**
 * A yes/NO dialog window to confirm if the user is meant to finish the annotation or not.
 *
 * @author Seid Muhie Yimam
 *
 */
public class ReCreateMergeCASModalPanel
    extends Panel
{

    private static final long serialVersionUID = 7771586567087376368L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private YesNoButtonsForm yesNoButtonsForm;

    private ReMergeCasModel reMerge;

    public ReCreateMergeCASModalPanel(String aId, ModalWindow aModalWindow, ReMergeCasModel aReMerege)
    {
        super(aId);
        this.reMerge = aReMerege;
        yesNoButtonsForm = new YesNoButtonsForm("yesNoButtonsForm", aModalWindow);
        add(yesNoButtonsForm);
    }

    private class YesNoButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -5659356972501634268L;

        public YesNoButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            add(new AjaxLink<Void>("yesButton")
            {
                private static final long serialVersionUID = -9043394507438053205L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    reMerge.setReMerege(true);
                    modalWindow.close(aTarget);

                }
            });
            add(new AjaxLink<Void>("noButton")
            {
                private static final long serialVersionUID = -9043394507438053205L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    reMerge.setReMerege(false);
                    modalWindow.close(aTarget);

                }
            });
        }
    }
}