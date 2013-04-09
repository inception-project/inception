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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;

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
import de.tudarmstadt.ukp.clarin.webanno.brat.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.dialog.OpenPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

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
    private OpenDocumentModel openDataMOdel;

    private CurationContainer curationContainer;

    // Open the dialog window on first load
    boolean firstLoad = true;

    public CurationPage()
    {
        openDataMOdel = new OpenDocumentModel();

        curationContainer = new CurationContainer();
        curationPanel = new CurationPanel("curationPanel", curationContainer);
        curationPanel.setOutputMarkupId(true);
        add(curationPanel);

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(350);
        openDocumentsModal.setInitialHeight(250);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                openDocumentsModal.setContent(new OpenPanel(openDocumentsModal.getContentId(),
                        openDataMOdel, openDocumentsModal));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    public void onClose(AjaxRequestTarget target)
                    {
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();

                        User user = repository.getUser(username);

                        if (openDataMOdel.getProject() != null
                                && openDataMOdel.getDocument() != null
                                && (openDataMOdel.getDocument().getState().toString()
                                        .equals(SourceDocumentState.ANNOTATION_FINISHED.toString()) || openDataMOdel
                                        .getDocument().getState().toString()
                                        .equals(SourceDocumentState.CURATION_INPROGRESS.toString()))) {

                            // Update source document state to CURRATION_INPROGRESS
                            openDataMOdel.getDocument().setState(
                                    SourceDocumentState.CURATION_INPROGRESS);
                            try {
                                repository.createSourceDocument(openDataMOdel.getDocument(), user);
                            }
                            catch (IOException e) {
                                error("Unable to update source document "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            // transform jcas to objects for wicket components
                            CurationBuilder builder = new CurationBuilder(repository);
                            curationContainer = builder.buildCurationContainer(openDataMOdel
                                    .getDocument());
                            curationContainer.setSourceDocument(openDataMOdel.getDocument());
                            updatePanel(curationContainer);
                            // target.add(curationPanel) should work!
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");

                            // target.add(curationPanel);
                        }
                        else {
                            target.appendJavaScript("alert('Annotation in progress!')");
                        }
                    }
                });
                openDocumentsModal.show(target);
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
