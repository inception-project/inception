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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class IRIValuePresenter
    extends ValuePresenter
{
    private static final long serialVersionUID = -2127902473859929221L;

    public IRIValuePresenter(String id, IModel<KBStatement> aModel)
    {
        super(id, aModel);

        LambdaAjaxLink link = new LambdaAjaxLink("link", this::actionIRILinkClicked);
        link.add(new Label("label", LambdaModel.of(this::getLabel)));
        add(link);
    }

    private String getLabel()
    {
        KBStatement stmt = getModelObject();
        return ((IRI) stmt.getValue()).getLocalName();
    }

    private void actionIRILinkClicked(AjaxRequestTarget target)
    {
        // TODO need to know what the IRI refers to - concept, property, both???
        KBStatement stmt = getModelObject();
        KBHandle selectedConcept = new KBHandle(((IRI) stmt.getValue()).toString());
        send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(target, selectedConcept));
    }
}
