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
package de.tudarmstadt.ukp.inception.ui.kb;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import java.util.Collection;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.eclipse.rdf4j.model.Statement;

import de.tudarmstadt.ukp.inception.bootstrap.dialog.ConfirmationDialogContentPanel_ImplBase;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;

public class DeleteKBObjectConfirmationDialogPanel
    extends ConfirmationDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = 7050666954726883326L;

    public DeleteKBObjectConfirmationDialogPanel(String aId,
            IModel<? extends KBObject> aHandleModel,
            IModel<Collection<Statement>> aStatementsWithReference)
    {
        super(aId);
        queue(new Label("name", aHandleModel.map(KBObject::getUiLabel)));
        Label sideEffects = new Label("sideEffects", new StringResourceModel("sideEffects")
                .setParameters(aStatementsWithReference.map(Collection::size)));
        sideEffects.add(visibleWhenNot(aStatementsWithReference.map(Collection::isEmpty)));
        queue(sideEffects);
    }
}
