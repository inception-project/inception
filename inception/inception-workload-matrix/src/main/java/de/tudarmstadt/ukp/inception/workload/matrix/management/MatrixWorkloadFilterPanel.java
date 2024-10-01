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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.FilterStateChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixFilterState;

public class MatrixWorkloadFilterPanel
    extends Panel
{
    private static final long serialVersionUID = 7581474359089251264L;

    public MatrixWorkloadFilterPanel(String aId, IModel<DocumentMatrixFilterState> aModel)
    {
        super(aId, aModel);

        var form = new Form<>("form", CompoundPropertyModel.of(aModel));
        add(form);

        form.add(new TextField<>("documentName", String.class));
        form.add(new IconToggleBox("matchDocumentNameAsRegex").setPostLabelText(Model.of("(.*)")));
        form.add(new TextField<>("userName", String.class));
        form.add(new IconToggleBox("matchUserNameAsRegex").setPostLabelText(Model.of("(.*)")));

        form.add(new LambdaAjaxButton<>("apply", this::onApplyFilter).triggerAfterSubmit());
        form.add(new LambdaAjaxButton<>("reset", this::onResetFilter).triggerAfterSubmit());
    }

    private void onApplyFilter(AjaxRequestTarget aTarget, Form<DocumentMatrixFilterState> aForm)
    {
        send(this, BUBBLE, new FilterStateChangedEvent(aTarget));
    }

    private void onResetFilter(AjaxRequestTarget aTarget, Form<DocumentMatrixFilterState> aForm)
    {
        getModelObject().reset();
        send(this, BUBBLE, new FilterStateChangedEvent(aTarget));
    }

    public DocumentMatrixFilterState getModelObject()
    {
        return (DocumentMatrixFilterState) getDefaultModelObject();
    }

    @SuppressWarnings("unchecked")
    public Model<DocumentMatrixFilterState> getModel()
    {
        return (Model<DocumentMatrixFilterState>) getDefaultModel();
    }
}
