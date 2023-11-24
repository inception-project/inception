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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewConceptEvent;

public class ConceptListPanel
    extends Panel
{
    private static final long serialVersionUID = -4032884234215283745L;
    private static final Logger LOG = LoggerFactory.getLogger(ConceptListPanel.class);

    private static final int LIST_MAX_ROWS = 30;

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KBHandle> selectedConcept;
    private IModel<KnowledgeBase> kbModel;
    private IModel<Preferences> preferences;

    public ConceptListPanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> selectedConceptModel)
    {
        super(aId, selectedConceptModel);

        setOutputMarkupId(true);

        selectedConcept = selectedConceptModel;
        kbModel = aKbModel;
        preferences = Model.of(new Preferences());

        OverviewListChoice<KBHandle> overviewList = new OverviewListChoice<>("concepts");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
        overviewList.setModel(selectedConceptModel);
        overviewList.setChoices(LoadableDetachableModel.of(this::getConcepts));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                this::actionSelectionChanged));
        overviewList.setMaxRows(LIST_MAX_ROWS);
        add(overviewList);

        add(new Label("count", overviewList.getChoicesModel().map(Collection::size)));

        LambdaAjaxLink addLink = new LambdaAjaxLink("add",
                target -> send(getPage(), Broadcast.BREADTH, new AjaxNewConceptEvent(target)));
        addLink.add(new Label("label", new ResourceModel("concept.list.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new CheckBox("showAllConcepts").add(
                new LambdaAjaxFormSubmittingBehavior("change", this::actionPreferenceChanged)));
        add(form);
    }

    private void actionSelectionChanged(AjaxRequestTarget aTarget)
    {
        // if the selection changes, publish an event denoting the change
        AjaxConceptSelectionEvent e = new AjaxConceptSelectionEvent(aTarget,
                selectedConcept.getObject());
        send(getPage(), Broadcast.BREADTH, e);
    }

    /**
     * If the user disabled "show all" but a concept from an implicit namespace was selected, the
     * concept selection is cancelled. In any other case this component is merely updated via AJAX.
     */
    private void actionPreferenceChanged(AjaxRequestTarget aTarget)
    {
        if (!preferences.getObject().showAllConcepts && selectedConcept.getObject() != null
                && IriConstants.isFromImplicitNamespace(selectedConcept.getObject())) {
            send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, null, true));
        }
        else {
            aTarget.add(this);
        }
    }

    private List<KBHandle> getConcepts()
    {
        if (!isVisibleInHierarchy()) {
            return Collections.emptyList();
        }

        Preferences prefs = preferences.getObject();
        try {
            return kbService.listAllConcepts(kbModel.getObject(), prefs.showAllConcepts);
        }
        catch (QueryEvaluationException e) {
            error("Unable to list concepts: " + e.getLocalizedMessage());
            LOG.error("Unable to list concepts.", e);
            return Collections.emptyList();
        }

    }

    static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = 8310379405075949753L;

        boolean showAllConcepts;
    }
}
