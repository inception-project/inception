/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoCssReference;

/**
 * Modal window to Export annotated document
 */
public class ExportModalWindowPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(ExportModalWindowPanel.class);

    @SpringBean(name = "importExportService")
    private ImportExportService importExportService;

    private class ExportDetailsForm
        extends Form<DefaultModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        private ArrayList<String> writeableFormats;

        private String selectedFormat;
        private DropDownChoice<String> documentChoice;

        private DropDownChoice<String> writeableFormatsChoice;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ExportDetailsForm(String id, final ModalWindow modalWindow)
        {
            super(id, new CompoundPropertyModel<DefaultModel>(new DefaultModel()));
            writeableFormats = (ArrayList<String>) importExportService.getWritableFormatLabels();
            selectedFormat = writeableFormats.get(0);
            add(writeableFormatsChoice = new DropDownChoice<String>("writeableFormats", new Model(
                    selectedFormat), writeableFormats));
            writeableFormatsChoice.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 226379059594234950L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    selectedFormat = writeableFormatsChoice.getModelObject();
                }
            });

            add(documentChoice = new DropDownChoice<String>("documentChoice",
                    Arrays.asList(new String[] { SELECTEXPORT.ANNOTATED.toString(),
                            SELECTEXPORT.AUTOMATED.toString() }))
            {
                private static final long serialVersionUID = -5565754860069220199L;

                @Override
                public boolean isVisible()
                {
                    return bratAnnotatorModel.getMode().equals(Mode.AUTOMATION);
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }

            });
            documentChoice.setModelObject(SELECTEXPORT.ANNOTATED.toString());
            documentChoice.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = -3853194405966729661L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    documentChoice.setModelObject(getModelObject().documentChoice);
                }
            });

            add(new Label("automated", "Export Annotated or Automated document :")
            {
                private static final long serialVersionUID = -6192738448954722290L;

                @Override
                public boolean isVisible()
                {
                    return bratAnnotatorModel.getMode().equals(Mode.AUTOMATION);
                }
            });

            add(new DownloadLink("export", new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    File downloadFile = null;
                    String username = bratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                            && getModelObject().documentChoice.equals(SELECTEXPORT.AUTOMATED
                                    .toString()) ? "CORRECTION_USER" : SecurityContextHolder
                            .getContext().getAuthentication().getName();
                    if (bratAnnotatorModel.getDocument() == null) {
                        getPage().error("NO Document is opened yet !");
                        throw new RestartResponseException(getPage());
                    }
                    else {
                        try {
                            downloadFile = importExportService.exportAnnotationDocument(
                                    bratAnnotatorModel.getDocument(),
                                    username,
                                    importExportService.getWritableFormats().get(
                                            importExportService.getWritableFormatId(selectedFormat)),
                                    bratAnnotatorModel.getDocument().getName(), bratAnnotatorModel
                                            .getMode());
                        }
                        catch (FileNotFoundException e) {
                            LOG.error("Export failed", e);
                            getSession().error("Unable to find annotation document:"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            // This will cause the open dialog to pop up again, but at least
                            // the error feedback message will be visible. With the 
                            // RestartResponseException the feedback message only flashes.
                            throw new NonResettingRestartException(getPage().getPageClass());
                        }
                        catch (UIMAException e) {
                            LOG.error("Export failed", e);
                            getSession().error("There is a problem while processing the CAS object:"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            // See comment above
                            throw new NonResettingRestartException(getPage().getPageClass());
                        }
                        catch (IOException e) {
                            LOG.error("Export failed", e);
                            getSession().error("Unable to find annotation document " + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            // See comment above
                            throw new NonResettingRestartException(getPage().getPageClass());
                        }
                        catch (ClassNotFoundException e) {
                            LOG.error("Export failed", e);
                            getSession().error("The class name in the properties is not found:"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            // See comment above
                            throw new NonResettingRestartException(getPage().getPageClass());
                        }
                        catch (Throwable e) {
                            LOG.error("Export failed", e);
                            getSession().error("Unexpected exception: "
                                    + ExceptionUtils.getRootCauseMessage(e));
                            // See comment above
                            throw new NonResettingRestartException(getPage().getPageClass());
                        }
                    }
                    return downloadFile;
                }
            }).setDeleteAfterDownload(true).setOutputMarkupId(true));

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

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // Loading WebAnno CSS because this doesn't inherit from ApplicationPageBase
        aResponse.render(CssHeaderItem.forReference(WebAnnoCssReference.get()));
    }
    
    public class DefaultModel
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;
        public String documentChoice;

    }

    private ExportDetailsForm exportForm;
    private AnnotatorState bratAnnotatorModel;

    public ExportModalWindowPanel(String aId, final ModalWindow modalWindow,
            AnnotatorState aBratAnnotatorModel)
    {
        super(aId);
        bratAnnotatorModel = aBratAnnotatorModel;
        exportForm = new ExportDetailsForm("exportForm", modalWindow);
        add(exportForm);
    }

    enum SELECTEXPORT
    {
        AUTOMATED, ANNOTATED;
    }

}
