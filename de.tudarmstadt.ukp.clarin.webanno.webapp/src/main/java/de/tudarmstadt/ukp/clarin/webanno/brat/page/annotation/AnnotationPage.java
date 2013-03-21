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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.annotation;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class AnnotationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private BratAnnotator annotator;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private DownloadLink export;
    private int windowSize;

    public AnnotationPage()
    {

        annotator = new BratAnnotator("embedder1", new Model<AnnotationDocument>());
        annotator.setOutputMarkupId(true);

        add(annotator);

        // Add a dialog panel to select annotation layers, window size and display lemma option
        final ModalWindow annotationLayerSelectionModal;
        add(annotationLayerSelectionModal = new ModalWindow("annotationLayerModal"));
        annotationLayerSelectionModal.setOutputMarkupId(true);

        annotationLayerSelectionModal.setInitialWidth(440);
        annotationLayerSelectionModal.setInitialHeight(250);
        annotationLayerSelectionModal.setResizable(true);
        annotationLayerSelectionModal.setWidthUnit("px");
        annotationLayerSelectionModal.setHeightUnit("px");
        annotationLayerSelectionModal
                .setTitle("Annotation Layer and window size configuration Window");

        add(new AjaxLink<Void>("showannotationLayerModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {

                    annotationLayerSelectionModal.setContent(new AnnotationLayerSelectionModalPage(
                            annotationLayerSelectionModal.getContentId(),
                            annotationLayerSelectionModal, annotator));

                    annotationLayerSelectionModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                public void onClose(AjaxRequestTarget target)
                                {
                                    // target.add(annotator);
                                    target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                                }
                            });
                    annotationLayerSelectionModal.show(target);
                }

            }
        });

        final ModalWindow exportModal;
        add(exportModal = new ModalWindow("exportModal"));

        exportModal.setCookieName("modal-1");
        exportModal.setInitialWidth(550);
        exportModal.setInitialHeight(450);
        exportModal.setResizable(true);
        exportModal.setWidthUnit("px");
        exportModal.setHeightUnit("px");
        exportModal.setTitle("Export Annotated data to a given Format");

        exportModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            public Page createPage()
            {
                return new ExportModalWindowPage(exportModal, annotator.getProject(), annotator
                        .getDocument());
            }

        });
        add(new AjaxLink<Void>("showExportModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getDocument() == null) {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
                else {
                    exportModal.show(target);
                }

            }
        });

        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getDocument() != null) {
                    int nextSentenceAddress = BratAjaxCasUtil
                            .getNextDisplayWindowSentenceBeginAddress(
                                    getJCas(annotator.getProject(), annotator.getDocument()),
                                    annotator.getSentenceAddress(), annotator.getWindowSize());
                    if (annotator.getSentenceAddress() != nextSentenceAddress) {
                        annotator.setSentenceAddress(nextSentenceAddress);
                        //target.add(annotator);
                         target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                    else {
                        target.appendJavaScript("alert('This is Last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getDocument() != null) {
                    int previousSentenceAddress = BratAjaxCasUtil
                            .getPreviousDisplayWindowSentenceBeginAddress(
                                    getJCas(annotator.getProject(), annotator.getDocument()),
                                    annotator.getSentenceAddress(), annotator.getWindowSize());
                    if (annotator.getSentenceAddress() != previousSentenceAddress) {
                        annotator.setSentenceAddress(previousSentenceAddress);
                       // target.add(annotator);
                         target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is First Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getDocument() != null) {
                    if (annotator.getFirstSentenceAddress() != annotator.getSentenceAddress()) {
                        annotator.setSentenceAddress(annotator.getFirstSentenceAddress());
                       // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is first Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.getDocument() != null) {
                    int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                            .getLastDisplayWindowFirstSentenceAddress(
                                    getJCas(annotator.getProject(), annotator.getDocument()),
                                    annotator.getWindowSize());
                    if (lastDisplayWindowBeginingSentenceAddress != annotator.getSentenceAddress()) {
                        annotator.setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);
                       // target.add(annotator);
                         target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });


        final ModalWindow guidelineModal;
        add(guidelineModal = new ModalWindow("guidelineModal"));

        guidelineModal.setInitialWidth(550);
        guidelineModal.setInitialHeight(450);
        guidelineModal.setResizable(true);
        guidelineModal.setWidthUnit("px");
        guidelineModal.setHeightUnit("px");
        guidelineModal.setTitle("Open Annotation Guideline, in separate window");

        guidelineModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            public Page createPage()
            {
                return new GuidelineModalWindowPage(guidelineModal, annotator.getProject());
            }

        });
        add(new AjaxLink<Void>("showGuidelineModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                    guidelineModal.show(target);


            }
        });

    }

    private JCas getJCas(Project aProject, SourceDocument aDocument)
    {
        JCas jCas = null;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = repository.getUser(username);
            BratAjaxCasController controller = new BratAjaxCasController(jsonConverter, repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return jCas;

    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

}
