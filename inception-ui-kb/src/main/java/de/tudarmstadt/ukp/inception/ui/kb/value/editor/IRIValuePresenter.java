/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;


public class IRIValuePresenter
    extends ValuePresenter
{
    private static final long serialVersionUID = -2127902473859929221L;

    private static final Logger LOG = LoggerFactory.getLogger(IRIValuePresenter.class);

    @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;

    public IRIValuePresenter(String id, IModel<KBStatement> aModel, IModel<KnowledgeBase> aKBModel)
    {
        super(id, aModel);
        kbModel = aKBModel;

        LambdaAjaxLink link = new LambdaAjaxLink("link",
            t -> actionIRILinkClicked(t, getKBObject()));
        link.add(new Label("label", Model.of(getLabel(getKBObject()))));
        add(link);
    }

    Optional<KBObject> getKBObject() {
        Object stmtValue = getModelObject().getValue();
        Optional<KBObject> kbObject = kbService
            .readKBIdentifier(kbModel.getObject(), stmtValue.toString());
        if (kbObject.isPresent()) {
            return kbObject;
        }
        return Optional.empty();
    }

    private String getLabel(Optional<KBObject> aKbObject)
    {
        if (aKbObject.isPresent()) {
            return aKbObject.get().getUiLabel();
        }
        return ((IRI) getModelObject().getValue()).getLocalName();
    }

    private void actionIRILinkClicked(AjaxRequestTarget aTarget, Optional<KBObject> aKbObject)
    {
        if (aKbObject.isPresent()) {
            KBObject kbObject = aKbObject.get();
            if (kbObject instanceof KBConcept) {
                send(getPage(), Broadcast.BREADTH,
                    new AjaxConceptSelectionEvent(aTarget, KBHandle.of(kbObject), true));
            }
            else if (kbObject instanceof KBInstance) {
                send(getPage(), Broadcast.BREADTH,
                    new AjaxInstanceSelectionEvent(aTarget, KBHandle.of(kbObject)));
            }
            else {
                send(getPage(), Broadcast.BREADTH,
                    new AjaxPropertySelectionEvent(aTarget, KBHandle.of(kbObject), true));
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
