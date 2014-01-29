/*******************************************************************************
 * Copyright 2014
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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A yes/NO dialog window to confirm if the user is meant to clear the annotation or not.
 *
 * @author Seid Muhie Yimam
 *
 */
public class ClearAnnotationModalPanel
    extends Panel
{

    private static final long serialVersionUID = 7771586567087376368L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private final YesNoButtonsForm yesNoButtonsForm;

    private final BratAnnotatorModel bratAnnotatorModel;
    private CheckBoxMultipleChoice clear;

    public ClearAnnotationModalPanel(String aId, BratAnnotatorModel aBratAnnotatorModel,
            ModalWindow aModalWindow)
    {
        super(aId);
        this.bratAnnotatorModel = aBratAnnotatorModel;
        yesNoButtonsForm = new YesNoButtonsForm("yesNoButtonsForm", aModalWindow);
        add(yesNoButtonsForm);
    }

    public static class SelectionModel
        implements Serializable
    {
        String clear;
    }

    private class YesNoButtonsForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -5659356972501634268L;

        public YesNoButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(clear = new CheckBoxMultipleChoice<String>("clear", Arrays.asList(new String[] {
                    "Annotator", "Automated" })));

            add(new AjaxSubmitLink("yesButton")
            {

                private static final long serialVersionUID = -2696545311438754743L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                    List<String> selected = Arrays.asList(clear.getModelObject().toString()
                            .replace("[", "").replace("]", "").split(","));

                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = repository.getUser(username);
                    if (selected.get(0).equals("")) {
                        modalWindow.close(aTarget);
                    }
                    else if (selected.size() == 2) {

                        try {
                            JCas annotatorJCas = repository.readJCas(bratAnnotatorModel
                                    .getDocument(), bratAnnotatorModel.getDocument().getProject(),
                                    user);

                            JCas automatedJCas = repository
                                    .getCorrectionDocumentContent(bratAnnotatorModel.getDocument());
                          BratAnnotatorUtility.clearJcasAnnotations(annotatorJCas,
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    repository);
                            BratAnnotatorUtility.clearJcasAutomated(automatedJCas,
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    repository);
                            modalWindow.close(aTarget);
                            bratAnnotatorModel.setAnnotationCleared(true);
                        }
                        catch (UIMAException e){
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (ClassNotFoundException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (IOException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                    }
                    else if (selected.get(0).equals("Annotator")) {
                        try {

                            JCas annotatorJCas = repository.readJCas(bratAnnotatorModel
                                    .getDocument(), bratAnnotatorModel.getDocument().getProject(),
                                    user);

                            BratAnnotatorUtility.clearJcasAnnotations(annotatorJCas,
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    repository);
                            modalWindow.close(aTarget);
                            bratAnnotatorModel.setAnnotationCleared(true);
                        }
                        catch (UIMAException e){
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (ClassNotFoundException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (IOException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                    }
                    else if (selected.get(0).equals("Automated")) {
                        try {
                            JCas automatedJCas = repository
                                    .getCorrectionDocumentContent(bratAnnotatorModel.getDocument());
                            BratAnnotatorUtility.clearJcasAutomated(automatedJCas,
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    repository);
                            modalWindow.close(aTarget);
                            bratAnnotatorModel.setAnnotationCleared(true);
                        }
                        catch (UIMAException e){
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (ClassNotFoundException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                        catch (IOException e) {
                            aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                        }
                    }
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
