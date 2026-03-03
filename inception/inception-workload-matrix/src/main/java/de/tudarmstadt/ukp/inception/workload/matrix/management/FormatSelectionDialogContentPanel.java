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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static java.lang.String.CASE_INSENSITIVE_ORDER;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class FormatSelectionDialogContentPanel
    extends Panel
{
    private static final long serialVersionUID = 1890467609404856741L;

    private @SpringBean DocumentImportExportService importExportService;

    private final LambdaAjaxLink cancelButton;
    private final AjaxPayloadCallback<String> confirmAction;

    public FormatSelectionDialogContentPanel(String aId, AjaxPayloadCallback<String> aConfirmAction)
    {
        super(aId);

        confirmAction = aConfirmAction;

        var writeableFormats = importExportService.getWritableFormats().stream()
                .map(fs -> new Format(fs.getId(), fs.getName())) //
                .sorted(Comparator.comparing(Format::name, CASE_INSENSITIVE_ORDER)) //
                .toList();

        queue(new Form<>("form", CompoundPropertyModel.of(new FormData())));

        queue(new DropDownChoice<>("format", writeableFormats) //
                .setChoiceRenderer(new LambdaChoiceRenderer<>(Format::name)) //
                .setRequired(true) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT)));

        queue(new LambdaAjaxButton<>("confirm", this::actionConfirm));

        cancelButton = new LambdaAjaxLink("cancel", this::actionCloseDialog);
        cancelButton.setOutputMarkupId(true);
        queue(cancelButton);

        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<FormData> aForm) throws Exception
    {
        confirmAction.accept(aTarget, aForm.getModelObject().format.id);
        actionCloseDialog(aTarget);
    }

    private void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    private record Format(String id, String name)
        implements Serializable
    {}

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -1;

        @SuppressWarnings("unused")
        public Format format;
    }
}
