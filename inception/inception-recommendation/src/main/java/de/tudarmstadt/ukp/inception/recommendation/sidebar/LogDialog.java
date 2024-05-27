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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;

public class LogDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = -6911254813496835955L;

    private IModel<List<LogMessageGroup>> logMessages;

    public LogDialog(String id)
    {
        this(id, null);
    }

    public LogDialog(String id, IModel<List<LogMessageGroup>> aModel)
    {
        super(id);

        trapFocus();

        logMessages = aModel;
    }

    public void setModel(IModel<List<LogMessageGroup>> aModel)
    {
        logMessages = aModel;
    }

    public void show(AjaxRequestTarget aTarget)
    {
        IModel<List<LogMessageGroup>> model = logMessages;

        if (model == null || model.getObject() == null) {
            LogMessageGroup group = new LogMessageGroup("No recommendations");
            group.setMessages(asList(LogMessage.info("", "No recommender run has completed yet.")));
            model = new ListModel<>(asList(group));
        }

        var content = new LogDialogContent(ModalDialog.CONTENT_ID, model);
        open(content, aTarget);
        aTarget.focusComponent(content.getFocusComponent());
    }
}
