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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ClearAnnotationModalPanel;

/**
 * A link to close/finish annotation/correction/curation project
 *
 * @author Seid Muhie Yimam
 */
public class ClearAnnotationLink
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    ModalWindow yesNoModal;

    public ClearAnnotationLink(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);

        final ModalWindow clearAnnotationModal;
        add(clearAnnotationModal = new ModalWindow("clearAnnotationModal"));
        clearAnnotationModal.setOutputMarkupId(true);

        clearAnnotationModal.setInitialWidth(400);
        clearAnnotationModal.setInitialHeight(100);
        clearAnnotationModal.setResizable(true);
        clearAnnotationModal.setWidthUnit("px");
        clearAnnotationModal.setHeightUnit("px");
        clearAnnotationModal.setTitle("Be aware that all annotations will be lost?");

        AjaxLink<Void> showClearAnnotationModal;

        add(showClearAnnotationModal = new AjaxLink<Void>("showClearAnnotationModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                clearAnnotationModal.setContent(new ClearAnnotationModalPanel(clearAnnotationModal
                        .getContentId(), aModel.getObject(), clearAnnotationModal));
                clearAnnotationModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        if(aModel.getObject().isAnnotationCleared()) {
                            onChange(target);
                        }
                    }
                });
                clearAnnotationModal.show(target);

            }
        });
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {

    }

}
