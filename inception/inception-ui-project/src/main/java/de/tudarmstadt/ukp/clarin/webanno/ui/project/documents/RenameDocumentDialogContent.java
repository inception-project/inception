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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Modal window to rename a source document
 */
public class RenameDocumentDialogContent
    extends GenericPanel<RenameDocumentDialogContent.FormData>
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean DocumentService documentService;

    private final IModel<SourceDocument> document;
    private final Form<FormData> form;
    private final LambdaAjaxLink cancelButton;
    private final AjaxPayloadCallback<String> renameAction;

    public RenameDocumentDialogContent(String aId, IModel<SourceDocument> aDocument,
            AjaxPayloadCallback<String> aRenameAction)
    {
        super(aId, new CompoundPropertyModel<>(new FormData()));

        renameAction = aRenameAction;
        document = aDocument;
        getModelObject().setName(aDocument.getObject().getName());

        form = new Form<>("form", getModel());
        add(form);

        queue(new TextField<String>("name") //
                .setRequired(true));

        queue(new LambdaAjaxButton<>("confirm", this::actionRename));

        cancelButton = new LambdaAjaxLink("cancel", this::actionCloseDialog);
        cancelButton.setOutputMarkupId(true);
        queue(cancelButton);

        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));
    }

    private void actionRename(AjaxRequestTarget aTarget, Form<FormData> aForm) throws Exception
    {
        renameAction.accept(aTarget, aForm.getModelObject().getName());
    }

    private void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    public void onShow(AjaxRequestTarget aTarget)
    {
        aTarget.focusComponent(cancelButton);
    }

    static final class FormData
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        String name;

        public String getName()
        {
            return name;
        }

        public void setName(String aName)
        {
            name = aName;
        }
    }
}
