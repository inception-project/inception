/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Comparator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementDetailPreference;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementGroupBean;

public class ConceptInfoPanel extends AbstractInfoPanel<KBConcept> {

    private static final long serialVersionUID = -8328024977043837787L;
    
    private @SpringBean KnowledgeBaseService kbService;

    public ConceptInfoPanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> handleModel, IModel<KBConcept> aModel) {
        super(aId, aKbModel, handleModel, aModel);
    }

    @Override
    protected void actionCreate(AjaxRequestTarget aTarget, Form<KBConcept> aForm) {
        KBConcept concept = kbObjectModel.getObject();

        assert isEmpty(concept.getIdentifier());
        KBHandle handle = kbService.createConcept(kbModel.getObject(), concept);

        // select newly created property right away to show the statements
        send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, handle, true));
    }

    @Override
    protected void actionDelete(AjaxRequestTarget aTarget) {
        kbService.deleteConcept(kbModel.getObject(), kbObjectModel.getObject());
        kbObjectModel.setObject(null);

        // send deselection event
        send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, null, true));
    }

    @Override
    protected void actionCancel(AjaxRequestTarget aTarget) {
        kbObjectModel.setObject(null);

        // send deselection event
        send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, null));
    }
    
    @Override
    protected String getTypeLabelResourceKey() {
        return "concept";
    }

    @Override
    protected String getNamePlaceholderResourceKey() {
        return "concept.new.placeholder";
    }
    
    @Override
    protected StatementDetailPreference getDetailPreference() {
        return StatementDetailPreference.ALL;
    }
    
    @Override
    protected Comparator<StatementGroupBean> getStatementGroupComparator() {
        return new ImportantStatementComparator(sgb -> {
            KnowledgeBase kb = kbModel.getObject();
            String identifier = sgb.getProperty().getIdentifier();
            return kb.getTypeIri().stringValue().equals(identifier) ||
                kb.getSubclassIri().stringValue().equals(identifier) ||
                kb.getLabelIri().stringValue().equals(identifier) ||
                kb.getDescriptionIri().stringValue().equals(identifier);
        });
    }
}
