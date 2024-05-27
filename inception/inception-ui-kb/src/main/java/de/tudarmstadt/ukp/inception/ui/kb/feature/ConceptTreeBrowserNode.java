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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptNavigateEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class ConceptTreeBrowserNode
    extends Folder<KBObject>
{
    private static final long serialVersionUID = 6706040390208486354L;

    private final NestedTree<KBObject> tree;
    private final IModel<KBObject> selectedConcept;
    private IModel<Boolean> conceptNavigationEnabled;
    private IModel<Boolean> conceptSelectionEnabled;

    public ConceptTreeBrowserNode(String aId, NestedTree<KBObject> aTree, IModel<KBObject> aModel,
            IModel<KBObject> aSelectedConcept, IModel<Boolean> aConceptNavigationEnabled,
            IModel<Boolean> aConceptSelectionEnabled)
    {
        super(aId, aTree, aModel);

        selectedConcept = aSelectedConcept;
        tree = aTree;
        conceptNavigationEnabled = aConceptNavigationEnabled;
        conceptSelectionEnabled = aConceptSelectionEnabled;

        queue(new LambdaAjaxLink("select", this::actionSelectConcept)
                .add(visibleWhen(() -> conceptNavigationEnabled.getObject()
                        && conceptSelectionEnabled.getObject())));
    }

    @Override
    protected IModel<String> newLabelModel(IModel<KBObject> aModel)
    {
        return Model.of(aModel.getObject().getUiLabel());
    }

    @Override
    protected boolean isClickable()
    {
        return true;
    }

    @Override
    protected void onClick(Optional<AjaxRequestTarget> aTarget)
    {
        if (conceptNavigationEnabled.getObject()) {
            actionViewConcept(aTarget.get());
        }
        else {
            actionSelectConcept(aTarget.get());
        }
    }

    private void actionViewConcept(AjaxRequestTarget aTarget)
    {
        if (selectedConcept.getObject() != null) {
            selectedConcept.detach();
            tree.updateNode(selectedConcept.getObject(), aTarget);
        }

        selectedConcept.setObject(getModelObject());
        tree.updateNode(selectedConcept.getObject(), aTarget);

        send(this, BUBBLE, new AjaxConceptNavigateEvent(aTarget, getModelObject().toKBHandle()));
    }

    private void actionSelectConcept(AjaxRequestTarget aTarget)
    {
        send(this, BUBBLE, new AjaxConceptSelectionEvent(aTarget, getModelObject().toKBHandle()));
    }

    @Override
    protected boolean isSelected()
    {
        var objectA = getModelObject();
        var objectB = selectedConcept.getObject();

        if (objectA == null || objectB == null) {
            return false;
        }

        return Objects.equals(objectA.getIdentifier(), objectB.getIdentifier());
    }
}
