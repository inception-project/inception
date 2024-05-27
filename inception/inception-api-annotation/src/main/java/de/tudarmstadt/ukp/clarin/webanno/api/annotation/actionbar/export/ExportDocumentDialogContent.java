/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.InputStreamResourceStream;

/**
 * Modal window to Export annotated document
 */
public class ExportDocumentDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean DocumentService documentService;

    private IModel<AnnotatorState> state;
    private IModel<Preferences> preferences;

    private final LambdaAjaxLink cancelButton;

    public ExportDocumentDialogContent(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId);
        state = aModel;

        var writeableFormats = importExportService.getWritableFormats().stream()
                .map(FormatSupport::getName) //
                .sorted(String.CASE_INSENSITIVE_ORDER) //
                .toList();

        var prefs = new Preferences();
        prefs.format = writeableFormats.get(0);

        preferences = Model.of(prefs);

        queue(new Form<>("form", CompoundPropertyModel.of(preferences)));

        var format = new DropDownChoice<String>("format", writeableFormats);
        format.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        queue(format);

        queue(new AjaxDownloadLink("confirm", //
                LoadableDetachableModel.of(this::export)));

        cancelButton = new LambdaAjaxLink("cancel", this::actionCloseDialog);
        cancelButton.setOutputMarkupId(true);
        queue(cancelButton);

        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));
    }

    private IResourceStream export()
    {
        File exportedFile = null;
        try {
            var s = state.getObject();
            var format = importExportService.getFormatByName(preferences.getObject().format).get();
            exportedFile = importExportService.exportAnnotationDocument(s.getDocument(),
                    s.getUser().getUsername(), format, s.getMode());

            var name = exportedFile.getName();

            // Safe-guard for legacy instances where document name validity has not been checked
            // during import.
            if (documentService.isValidDocumentName(s.getDocument().getName())) {
                name = FilenameUtils.getBaseName(s.getDocument().getName()) + "."
                        + FilenameUtils.getExtension(exportedFile.getName());
            }

            var resource = new InputStreamResourceStream(new FileInputStream(exportedFile), name);

            var cleaner = Application.get().getResourceSettings().getFileCleaner();
            cleaner.track(exportedFile, resource);

            return resource;
        }
        catch (Exception e) {
            LOG.error("Export failed", e);
            getSession().error("Export failed: " + ExceptionUtils.getRootCauseMessage(e));
            if (exportedFile != null) {
                exportedFile.delete();
            }
            return null;
        }
    }

    protected void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    public void onShow(AjaxRequestTarget aTarget)
    {
        aTarget.focusComponent(cancelButton);
    }

    private static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;

        public String format;
    }
}
