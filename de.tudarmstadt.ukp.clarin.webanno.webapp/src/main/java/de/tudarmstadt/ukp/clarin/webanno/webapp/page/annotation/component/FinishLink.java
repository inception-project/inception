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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;

/**
 * A link to close/finish annotation/correction/curation project
 * @author Seid Muhie Yimam
 */
public class FinishLink
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    ModalWindow yesNoModal;

    public FinishLink(String id, final IModel<BratAnnotatorModel> aModel, final FinishImage finishImag)
    {
        super(id, aModel);

        final ModalWindow FinishModal;
        add(FinishModal = new ModalWindow("yesNoModal"));
        FinishModal.setOutputMarkupId(true);

        FinishModal.setInitialWidth(400);
        FinishModal.setInitialHeight(50);
        FinishModal.setResizable(true);
        FinishModal.setWidthUnit("px");
        FinishModal.setHeightUnit("px");
        FinishModal.setTitle("Are you sure you want to finish annotating?");

        AjaxLink<Void> showYesNoModal;

        add(showYesNoModal = new AjaxLink<Void>("showYesNoModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);
                if (repository.getAnnotationDocument(aModel.getObject().getDocument(), user)
                        .getState().equals(AnnotationDocumentState.FINISHED)) {
                    target.appendJavaScript("alert('Document already closed!')");
                }
                else {
                    FinishModal.setContent(new YesNoModalPanel(FinishModal.getContentId(),
                            aModel.getObject(), FinishModal, Mode.ANNOTATION));
                    FinishModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                    {
                        private static final long serialVersionUID = -1746088901018629567L;

                        @Override
                        public void onClose(AjaxRequestTarget target)
                        {
                            target.add(finishImag.setOutputMarkupId(true));
                        }
                    });
                    FinishModal.show(target);
                }

            }
        });
        showYesNoModal.add(finishImag);
    }

}
