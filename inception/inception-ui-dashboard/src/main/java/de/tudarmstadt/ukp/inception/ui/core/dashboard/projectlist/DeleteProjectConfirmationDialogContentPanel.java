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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.bootstrap.dialog.ChallengeResponseDialogContentPanel_ImplBase;

public class DeleteProjectConfirmationDialogContentPanel
    extends ChallengeResponseDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = -943392917974988048L;

    public DeleteProjectConfirmationDialogContentPanel(String aId)
    {
        this(aId, Model.of(emptyList()));
    }

    public DeleteProjectConfirmationDialogContentPanel(String aId,
            IModel<Collection<Project>> aIModel)
    {
        super(aId, new ResourceModel("title"));

        if (aIModel.map(c -> c.size() > 1).orElse(false).getObject()) {
            queue(new Label("message", new StringResourceModel("message.multiple")
                    .setParameters(aIModel.map(Collection::size))));
            queue(new Label("challenge", new ResourceModel("challenge.multiple")));
        }
        else {
            queue(new Label("message", new ResourceModel("message.single")));
            queue(new Label("challenge", new ResourceModel("challenge.single")));
        }
    }
}
