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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.finish;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static java.util.Arrays.asList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.StringValidator;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxFormCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class FinishDocumentDialogContent
    extends Panel
{
    private static final long serialVersionUID = -3061603183575062122L;

    private static final int MAX_COMMENT_LENGTH = 2000;

    private Form<FinishDocumentDialogModel> form;

    public FinishDocumentDialogContent(String aId, IModel<FinishDocumentDialogModel> aModel,
            AjaxFormCallback<FinishDocumentDialogModel> aCallback)
    {
        super(aId, aModel);

        form = new Form<>("form", CompoundPropertyModel.of(aModel));

        var state = new BootstrapRadioChoice<AnnotationDocumentState>("state",
                asList(FINISHED, IGNORE));
        state.setEscapeModelStrings(false); // SAFE - RENDERING ONLY CONTROLLED SET OF ICONS
        state.setChoiceRenderer(new EnumChoiceRenderer<>(state));
        form.add(state);
        form.add(new TextArea<>("comment").add(new StringValidator(0, MAX_COMMENT_LENGTH)));
        form.add(new LambdaAjaxButton<>("confirm", aCallback));
        form.add(new LambdaAjaxLink("cancel", this::actionCloseDialog));
        form.add(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));

        add(form);
    }

    @SuppressWarnings("unchecked")
    public IModel<FinishDocumentDialogContent> getModel()
    {
        return (IModel<FinishDocumentDialogContent>) getDefaultModel();
    }

    public FinishDocumentDialogContent getModelObject()
    {
        return (FinishDocumentDialogContent) getDefaultModelObject();
    }

    protected void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
