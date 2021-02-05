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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
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

    private @SpringBean ImportExportService importExportService;

    private IModel<AnnotatorState> state;
    private IModel<Preferences> preferences;

    public ExportDocumentDialogContent(String aId, final ModalWindow modalWindow,
            IModel<AnnotatorState> aModel)
    {
        super(aId);
        state = aModel;

        List<String> writeableFormats = importExportService.getWritableFormats().stream()
                .map(FormatSupport::getName).sorted().collect(Collectors.toList());

        Preferences prefs = new Preferences();
        prefs.format = writeableFormats.get(0);
        prefs.documentType = SELECTEXPORT.ANNOTATED.toString();

        preferences = Model.of(prefs);

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        add(form);

        DropDownChoice<String> format = new BootstrapSelect<>("format", writeableFormats);
        format.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        form.add(format);

        // FIXME Use EnumChoiceRenderer?
        DropDownChoice<String> documentType = new BootstrapSelect<>("documentType",
                Model.of(SELECTEXPORT.ANNOTATED.toString()), Arrays.asList(
                        SELECTEXPORT.ANNOTATED.toString(), SELECTEXPORT.AUTOMATED.toString()));
        documentType.setVisible(state.getObject().getMode().equals(Mode.AUTOMATION));
        documentType.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        form.add(documentType);

        AjaxDownloadLink export = new AjaxDownloadLink("export",
                LoadableDetachableModel.of(this::export));
        form.add(export);
        form.add(new LambdaAjaxLink("cancel", (target) -> modalWindow.close(target)));
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
            error("Export failed:" + ExceptionUtils.getRootCauseMessage(e));
            // This will cause the open dialog to pop up again, but at least
            // the error feedback message will be visible. With the
            // RestartResponseException the feedback message only flashes.
            throw new NonResettingRestartException(getPage().getPageClass());
        }
    }

    private static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;

        public String documentType;
        public String format;

    }

    private static enum SELECTEXPORT
    {
        AUTOMATED, ANNOTATED
    }
}
