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

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.apache.wicket.util.resource.FileResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;

/**
 * Modal window to Export annotated document
 */
public class ExportDocumentDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(ExportDocumentDialogContent.class);

    private @SpringBean DocumentImportExportService importExportService;

    private IModel<AnnotatorState> state;
    private IModel<Preferences> preferences;

    private final LambdaAjaxLink cancelButton;

    public ExportDocumentDialogContent(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId);
        state = aModel;

        List<String> writeableFormats = importExportService.getWritableFormats().stream()
                .map(FormatSupport::getName) //
                .sorted() //
                .collect(toList());

        Preferences prefs = new Preferences();
        prefs.format = writeableFormats.get(0);

        preferences = Model.of(prefs);

        queue(new Form<>("form", CompoundPropertyModel.of(preferences)));

        DropDownChoice<String> format = new DropDownChoice<>("format", writeableFormats);
        format.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        queue(format);

        queue(new AjaxDownloadLink("confirm", LoadableDetachableModel.of(this::export)));
        cancelButton = new LambdaAjaxLink("cancel", this::actionCloseDialog);
        cancelButton.setOutputMarkupId(true);
        queue(cancelButton);
        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));
    }

    private FileResourceStream export()
    {
        try {
            return new FileResourceStream(importExportService.exportAnnotationDocument(
                    state.getObject().getDocument(), state.getObject().getUser().getUsername(),
                    importExportService.getFormatByName(preferences.getObject().format).get(),
                    state.getObject().getDocument().getName(), state.getObject().getMode()));
        }
        catch (Exception e) {
            LOG.error("Export failed", e);
            getSession().error("Export failed: " + ExceptionUtils.getRootCauseMessage(e));
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
