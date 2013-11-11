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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Modal window to Export annotated document
 *
 * @author Seid Muhie Yimam
 *
 */
public class ExportModalWindowPage
    extends WebPage
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(ExportModalWindowPage.class);

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private class ExportDetailsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        private DownloadLink export;

        private ArrayList<String> writeableFormats;

        private String selectedFormat;

        private DropDownChoice<String> writeableFormatsChoice;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ExportDetailsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            try {
                writeableFormats = (ArrayList<String>) repository.getWritableFormatLabels();
                selectedFormat = writeableFormats.get(0);
            }
            catch (IOException e) {
                error("Properties file not found or key not int the properties file" + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
            }
            catch (ClassNotFoundException e) {
                error("The Class name in the properties is not found " + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
            }
            add(new FeedbackPanel("feedbackPanel"));
            add(writeableFormatsChoice = new DropDownChoice<String>("writeableFormats", new Model(
                    selectedFormat), writeableFormats));
            writeableFormatsChoice.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 226379059594234950L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    selectedFormat = writeableFormatsChoice.getModelObject();
                }
            });

            add(export = (DownloadLink) new DownloadLink("export",
                    new LoadableDetachableModel<File>()
                    {
                        private static final long serialVersionUID = 840863954694163375L;

                        @Override
                        protected File load()
                        {
                            File downloadFile = null;

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = repository.getUser(username);
                            if (bratAnnotatorModel.getDocument() == null) {
                                error("NO Document is opened yet !");
                            }
                            else {

                                try {
                                    downloadFile = repository.exportAnnotationDocument(
                                            bratAnnotatorModel.getDocument(),
                                            username,
                                            repository.getWritableFormats().get(
                                                    repository.getWritableFormatId(selectedFormat)),
                                            bratAnnotatorModel.getDocument().getName(),
                                            bratAnnotatorModel.getMode());
                                }
                                catch (FileNotFoundException e) {
                                    error("Ubable to find annotation document " + ":"
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                                catch (UIMAException e) {
                                    error("There is a proble while processing the CAS object "
                                            + ":" + ExceptionUtils.getRootCauseMessage(e));
                                }
                                catch (IOException e) {
                                    error("Ubable to find annotation document " + ":"
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                                catch (ClassNotFoundException e) {
                                    error("The Class name in the properties is not found " + ":"
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }

                            }
                            return downloadFile;
                        }
                    }).setOutputMarkupId(true));

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

    private ExportDetailsForm exportForm;
    private BratAnnotatorModel bratAnnotatorModel;

    public ExportModalWindowPage(final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        this.bratAnnotatorModel = aBratAnnotatorModel;
        exportForm = new ExportDetailsForm("exportForm", modalWindow);
        add(exportForm);
    }

}
