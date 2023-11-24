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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

import de.tudarmstadt.ukp.inception.bootstrap.dialog.ChallengeResponseDialogContentPanel_ImplBase;

public class ResetCurationDocumentConfirmationDialogContentPanel
    extends ChallengeResponseDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = -943392917974988048L;

    public ResetCurationDocumentConfirmationDialogContentPanel(String aId)
    {
        super(aId, new ResourceModel("title"));
        queue(new Label("message", new ResourceModel("message")));
    }
}
