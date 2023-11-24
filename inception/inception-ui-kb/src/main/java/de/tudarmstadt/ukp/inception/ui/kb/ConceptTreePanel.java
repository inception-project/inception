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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewConceptEvent;

public class ConceptTreePanel
    extends Panel
{
    private static final long serialVersionUID = -4032884234215283745L;

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KBObject> selectedConcept;
    private IModel<KnowledgeBase> kbModel;
    private IModel<ConceptTreeProviderOptions> options;

    public ConceptTreePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBObject> selectedConceptModel)
    {
        super(aId, selectedConceptModel);

        setOutputMarkupId(true);

        selectedConcept = selectedConceptModel;
        kbModel = aKbModel;
        options = Model.of(new ConceptTreeProviderOptions());

        AbstractTree<KBObject> tree = new DefaultNestedTree<KBObject>("tree",
                new ConceptTreeProvider(kbService, kbModel, options), Model.ofSet(new HashSet<>()))
        {
            private static final long serialVersionUID = -270550186750480253L;

            @Override
            protected Component newContentComponent(String id, IModel<KBObject> node)
            {
                return new Folder<KBObject>(id, this, node)
                {
                    private static final long serialVersionUID = -2007320226995118959L;

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
                        if (selectedConcept.getObject() != null) {
                            selectedConcept.detach();
                            updateNode(selectedConcept.getObject(), aTarget.get());
                        }
                        selectedConcept.setObject(getModelObject());
                        updateNode(selectedConcept.getObject(), aTarget.get());
                        actionSelectionChanged(aTarget.get());
                    }

                    @Override
                    protected boolean isSelected()
                    {
                        return Objects.equals(getModelObject(), selectedConcept.getObject());
                    }
                };
            }
        };
        add(tree);

        LambdaAjaxLink addLink = new LambdaAjaxLink("add",
                target -> send(getPage(), Broadcast.BREADTH, new AjaxNewConceptEvent(target)));
        addLink.add(new Label("label", new ResourceModel("concept.list.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);

        Form<ConceptTreeProviderOptions> form = new Form<>("form",
                CompoundPropertyModel.of(options));
        form.add(new CheckBox("showAllConcepts").setOutputMarkupId(true) //
                .add(new LambdaAjaxFormSubmittingBehavior("change",
                        this::actionPreferenceChanged)));
        add(form);
    }

    private void actionSelectionChanged(AjaxRequestTarget aTarget)
    {
        // if the selection changes, publish an event denoting the change
        AjaxConceptSelectionEvent e = new AjaxConceptSelectionEvent(aTarget,
                selectedConcept.getObject().toKBHandle());
        send(getPage(), Broadcast.BREADTH, e);
    }

    /**
     * If the user disabled "show all" but a concept from an implicit namespace was selected, the
     * concept selection is cancelled. In any other case this component is merely updated via AJAX.
     */
    private void actionPreferenceChanged(AjaxRequestTarget aTarget)
    {
        if (!options.getObject().isShowAllConcepts() && selectedConcept.getObject() != null
                && IriConstants.isFromImplicitNamespace(selectedConcept.getObject())) {
            send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, null, true));
        }
        else {
            aTarget.add(this);
        }
    }
}
