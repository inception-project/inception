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
package de.tudarmstadt.ukp.inception.workload.ui;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class ProjectProgressDialogContentPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 6891424312377039938L;

    public ProjectProgressDialogContentPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        add(new ProjectProgressPanel("progressPanel", aModel));
        add(new LambdaAjaxLink("closeDialog", this::onCancel) //
                .setOutputMarkupId(true));
    }

    private void onCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
