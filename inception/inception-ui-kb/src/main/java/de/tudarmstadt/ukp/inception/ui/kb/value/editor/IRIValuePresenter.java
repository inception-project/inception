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
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.IriInfoBadge;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;

public class IRIValuePresenter
    extends ValuePresenter
{
    private static final long serialVersionUID = -2127902473859929221L;

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;

    public IRIValuePresenter(String id, IModel<KBStatement> aModel, IModel<KnowledgeBase> aKBModel)
    {
        super(id, aModel);

        kbModel = aKBModel;

        add(new IriInfoBadge("iriInfoBadge", LoadableDetachableModel
                .of(() -> ((IRI) getModelObject().getValue()).stringValue())));

        LambdaAjaxLink link = new LambdaAjaxLink("link",
                t -> actionIRILinkClicked(t, (IRI) aModel.getObject().getValue()));
        link.add(
                new Label("label", LoadableDetachableModel.of(() -> getLabel(aModel.getObject()))));
        add(link);
    }

    private String getLabel(KBStatement aStatement)
    {
        if (aStatement == null) {
            return null;
        }

        if (aStatement != null && aStatement.getValueLabel() != null) {
            return aStatement.getValueLabel();
        }

        return ((IRI) aStatement.getValue()).getLocalName();
    }

    private void actionIRILinkClicked(AjaxRequestTarget aTarget, IRI aIdentifier)
    {
        KBObject item = kbService.readItem(kbModel.getObject(), aIdentifier.stringValue())
                .orElse(null);

        if (item != null) {
            if (item instanceof KBConcept) {
                send(getPage(), Broadcast.BREADTH,
                        new AjaxConceptSelectionEvent(aTarget, KBHandle.of(item), true));
            }
            else if (item instanceof KBInstance) {
                send(getPage(), Broadcast.BREADTH,
                        new AjaxInstanceSelectionEvent(aTarget, KBHandle.of(item)));
            }
            else if (item instanceof KBProperty) {
                send(getPage(), Broadcast.BREADTH,
                        new AjaxPropertySelectionEvent(aTarget, (KBProperty) item, true));
            }
            else {
                throw new IllegalArgumentException(String.format(
                        "KBObject must be an instance of one of the following types: [KBConcept, KBInstance, KBProperty], not [%s]",
                        item.getClass().getSimpleName()));
            }
        }
        else {
            // send concept selection changed event to display the resource in concept info panel
            KBStatement stmt = getModelObject();
            KBHandle selectedConcept = new KBHandle(((IRI) stmt.getValue()).toString());
            send(getPage(), Broadcast.BREADTH,
                    new AjaxConceptSelectionEvent(aTarget, selectedConcept, true));
        }

    }
}
