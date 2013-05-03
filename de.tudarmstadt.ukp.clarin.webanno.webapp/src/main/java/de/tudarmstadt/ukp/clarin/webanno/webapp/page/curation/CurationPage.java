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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationDocumentVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

/**
 *
 * @author Andreas Straninger This is the main class for the curation page. It contains an interface
 *         which displays differences between user annotations for a specific document. The
 *         interface provides a tool for merging these annotations and storing them as a new
 *         annotation.
 */
public class CurationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private AjaxLink<Void> reload1;

    private AjaxLink<Void> reload2;

    private BratAnnotationDocumentVisualizer embedder1;

    private BratAnnotationDocumentVisualizer embedder2;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;
    private CurationPanel curationPanel;
    private OpenDocumentModel openDataModel;

    private CurationContainer curationContainer;

    // Open the dialog window on first load
    boolean firstLoad = true;

    public CurationPage()
    {
        openDataModel = new OpenDocumentModel();

        curationContainer = new CurationContainer();
        curationPanel = new CurationPanel("curationPanel", curationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(500);
        openDocumentsModal.setInitialHeight(300);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal.getContentId(),
                        openDataModel, openDocumentsModal, Mode.CURATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        User user = repository.getUser(username);
                        // If this source document has at least one annotation document "FINISHED",
                        // and curation not yet
                        // finished on it
                        if (openDataModel.getDocument() != null
                                && repository.existsFinishedAnnotation(openDataModel.getDocument(),
                                        openDataModel.getProject())
                                && !openDataModel.getDocument().getState()
                                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                            // Update source document state to CURRATION_INPROGRESS
                            openDataModel.getDocument().setState(
                                    SourceDocumentState.CURATION_INPROGRESS);
                            try {
                                repository.createSourceDocument(openDataModel.getDocument(), user);
                            }
                            catch (IOException e) {
                                error("Unable to update source document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            // transform jcas to objects for wicket components
                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(openDataModel.getProject(), openDataModel
                                    .getDocument());
                            curationContainer.setSourceDocument(openDataModel.getDocument());
                            curationContainer.setProject(openDataModel.getProject());
                            updatePanel(curationContainer);
                            // target.add(curationPanel) should work!
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                        }
                        else if (openDataModel.getDocument() != null
                                && openDataModel.getDocument().getState()
                                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                            target.appendJavaScript("alert('Curation Has been closed. Ask admin to re-open!')");
                        }
                        else if (openDataModel.getDocument() == null) {
                            setResponsePage(WelcomePage.class);
                        }
                        else {
                            target.appendJavaScript("alert('Annotation in progress for document ["
                                    + openDataModel.getDocument().getName() + "]')");
                        }
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        final ModalWindow yesNoModal;
        add(yesNoModal = new ModalWindow("yesNoModal"));
        yesNoModal.setOutputMarkupId(true);

        yesNoModal.setInitialWidth(400);
        yesNoModal.setInitialHeight(50);
        yesNoModal.setResizable(true);
        yesNoModal.setWidthUnit("px");
        yesNoModal.setHeightUnit("px");
        yesNoModal.setTitle("Are you sure you want to finish curating?");

        add(new AjaxLink<Void>("showYesNoModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                yesNoModal.setContent(new YesNoModalPanel(yesNoModal.getContentId(), openDataModel,
                        yesNoModal, Mode.CURATION));
                yesNoModal.show(target);
            }
        });
    }

    // Update the curation panel.
    private void updatePanel(CurationContainer aCurationContainer)
    {
        // remove old panel, create new one, add it
        remove(curationPanel);
        curationPanel = new CurationPanel("curationPanel", aCurationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.renderOnLoadJavaScript(jQueryString);
    }
}
