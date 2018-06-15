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
package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class IRIValuePresenter<T extends IRI>
    extends Panel
    implements ValuePresenter<T>
{
    private static final long serialVersionUID = -2127902473859929221L;
    
    private IModel<String> stringModel;
    private IModel<T> iriModel;

    public IRIValuePresenter(String id, IModel<T> model) {
        super(id, model);
        
        iriModel = model;
        stringModel = Model.of();
        
        LambdaAjaxLink link = new LambdaAjaxLink("link", this::actionIRILinkClicked);
        link.add(new Label("label", stringModel));
        add(link);
    }
    
    @Override
    protected void onBeforeRender() {
        Object object = getDefaultModelObject();
        
        // if the model provides what it promises
        if (object instanceof IRI) {
            IRI iri = (IRI) object;
            stringModel.setObject(iri.getLocalName());
        } else {
            stringModel.setObject(null);
        }
        super.onBeforeRender();
    }
    
    private void actionIRILinkClicked(AjaxRequestTarget target) {
        // TODO need to know what the IRI refers to - concept, property, both???
        T selectedIRI = iriModel.getObject();
        KBHandle selectedConcept = new KBHandle(selectedIRI.stringValue());
        send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(target, selectedConcept));
    }
}
