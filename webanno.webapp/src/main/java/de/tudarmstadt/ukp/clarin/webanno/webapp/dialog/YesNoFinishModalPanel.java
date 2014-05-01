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

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A yes/NO dialog window to confirm if the user is meant to finish the annotation or not.
 *
 * @author Seid Muhie Yimam
 *
 */
public class YesNoFinishModalPanel
    extends Panel
{

    private static final long serialVersionUID = 7771586567087376368L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private YesNoButtonsForm yesNoButtonsForm;

    private BratAnnotatorModel bratAnnotatorModel;

    public YesNoFinishModalPanel(String aId, BratAnnotatorModel aOpenDocumentModel, ModalWindow aModalWindow,
            Mode aSubject)
    {
        super(aId);
        this.bratAnnotatorModel = aOpenDocumentModel;
        yesNoButtonsForm = new YesNoButtonsForm("yesNoButtonsForm", aModalWindow, aSubject);
        add(yesNoButtonsForm);
    }

    private class YesNoButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -5659356972501634268L;

        public YesNoButtonsForm(String id, final ModalWindow modalWindow, final Mode aSubject)
        {
            super(id);
            add(new AjaxSubmitLink("yesButton")
            {

                private static final long serialVersionUID = -2696545311438754743L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();

                    User user = repository.getUser(username);

                    if (aSubject.equals(Mode.ANNOTATION)) {
                        AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                                bratAnnotatorModel.getDocument(), user);

                        annotationDocument
                                .setState(AnnotationDocumentStateTransition
                                        .transition(AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED));
                        // manually update state change!! No idea why it is not updated in the DB
                        // with
                        // out calling
                        // createAnnotationDocument(...)
                        try {
                            repository.createAnnotationDocument(annotationDocument);
                        }
                        catch (IOException e) {
                           error("Unable to get the LOG file");
                        }

                    }
                    else {

                        bratAnnotatorModel.getDocument().setState(
                                SourceDocumentState.CURATION_FINISHED);
                        bratAnnotatorModel.getDocument().setProcessed(false);
                        try {
                            repository.createSourceDocument(bratAnnotatorModel.getDocument(), user);
                        }
                        catch (IOException e) {
                            error("Unable to update source file "
                                    + ExceptionUtils.getRootCauseMessage(e));

                        }
                    }

                    modalWindow.close(aTarget);
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("noButton")
            {
                private static final long serialVersionUID = -9043394507438053205L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    modalWindow.close(aTarget);

                }
            });
        }
    }
}
