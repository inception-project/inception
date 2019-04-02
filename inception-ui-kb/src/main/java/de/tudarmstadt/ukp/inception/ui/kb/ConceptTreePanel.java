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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewConceptEvent;

public class ConceptTreePanel extends Panel {
    private static final long serialVersionUID = -4032884234215283745L;
    private static final Logger LOG = LoggerFactory.getLogger(ConceptTreePanel.class);
    
    private @SpringBean KnowledgeBaseService kbService;
    
    private IModel<KBHandle> selectedConcept;
    private IModel<KnowledgeBase> kbModel;
    private IModel<Preferences> preferences;
    
    public ConceptTreePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> selectedConceptModel) {
        super(aId, selectedConceptModel);
        
        setOutputMarkupId(true);
        
        selectedConcept = selectedConceptModel;
        kbModel = aKbModel;
        preferences = Model.of(new Preferences());
        
        AbstractTree<KBHandle> tree = new DefaultNestedTree<KBHandle>("tree",
                new ConceptTreeProvider(), Model.ofSet(new HashSet<>()))
        {
            private static final long serialVersionUID = -270550186750480253L;

            @Override
            protected Component newContentComponent(String id, IModel<KBHandle> node)
            {
                return new Folder<KBHandle>(id, this, node) {
                    private static final long serialVersionUID = -2007320226995118959L;

                    @Override
                    protected IModel<String> newLabelModel(IModel<KBHandle> aModel)
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
        
        LambdaAjaxLink addLink = new LambdaAjaxLink("add", target -> send(getPage(),
                Broadcast.BREADTH, new AjaxNewConceptEvent(target)));
        addLink.add(new Label("label", new ResourceModel("concept.list.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new CheckBox("showAllConcepts").add(
                new LambdaAjaxFormSubmittingBehavior("change", this::actionPreferenceChanged)));
        add(form);
    }
    
    private void actionSelectionChanged(AjaxRequestTarget aTarget) {
        // if the selection changes, publish an event denoting the change
        AjaxConceptSelectionEvent e = new AjaxConceptSelectionEvent(aTarget,
                selectedConcept.getObject());
        send(getPage(), Broadcast.BREADTH, e);
    }
    
    /**
     * If the user disabled "show all" but a concept from an implicit namespace was selected, the
     * concept selection is cancelled. In any other case this component is merely updated via AJAX.
     */
    private void actionPreferenceChanged(AjaxRequestTarget aTarget) {
        if (!preferences.getObject().showAllConcepts && selectedConcept.getObject() != null
                && IriConstants.isFromImplicitNamespace(selectedConcept.getObject())) {
            send(getPage(), Broadcast.BREADTH, new AjaxConceptSelectionEvent(aTarget, null, true));
        } else {
            aTarget.add(this);
        }
    }
    
    private class ConceptTreeProvider implements ITreeProvider<KBHandle>
    {
        private static final long serialVersionUID = 5318498575532049499L;

        private Map<KBHandle, Boolean> childrenPresentCache = new HashMap<>();
        private Map<KBHandle, List<KBHandle>> childrensCache = new HashMap<>();
        
        @Override
        public void detach()
        {
            if (!kbModel.getObject().isReadOnly()) {
                childrenPresentCache.clear();
            }
        }

        @Override
        public Iterator<? extends KBHandle> getRoots()
        {
            try {
                return kbService.listRootConcepts(kbModel.getObject(),
                        preferences.getObject().showAllConcepts).iterator();
            } catch (QueryEvaluationException e) {
                error(getString("listRootConceptsErrorMsg") + ": " + e.getLocalizedMessage());
                LOG.error("Unable to list root concepts.", e);
                return Collections.emptyIterator();
            }
        }

        @Override
        public boolean hasChildren(KBHandle aNode)
        {
            try {
                // If the KB is read-only, then we cache the values and re-use the cached values.
                if (kbModel.getObject().isReadOnly()) {
                    // Leaving this code here because we might make the preemptive loading of 
                    // child presence optional.
    //                Boolean childrenPresent = childrenPresentCache.get(aNode);
    //                if (childrenPresent == null) {
    //                    childrenPresent = kbService.hasChildConcepts(kbModel.getObject(),
    //                            aNode.getIdentifier(), preferences.getObject().showAllConcepts);
    //                    childrenPresentCache.put(aNode, childrenPresent);
    //                }
    //                return childrenPresent;
                    
                    // To avoid having to send a query to the KB for every child node, just assume
                    // that there might be child nodes and show the expander until we have actually
                    // loaded the children, cached them and can show the true information.
                    List<KBHandle> children = childrensCache.get(aNode);
                    if (children == null) {
                        return true;
                    }
                    else {
                        return !children.isEmpty();
                    }
                }
                else {
                    Boolean hasChildren = childrenPresentCache.get(aNode);
                    if (hasChildren == null) {
                        hasChildren = kbService.hasChildConcepts(kbModel.getObject(),
                                aNode.getIdentifier(), preferences.getObject().showAllConcepts);
                        childrenPresentCache.put(aNode, hasChildren);
                    }
                    
                    return hasChildren;
                }
            }
            catch (QueryEvaluationException e) {
                error(getString("listChildConceptsErrorMsg") + ": " + e.getLocalizedMessage());
                LOG.error("Unable to list child concepts.", e);
                return false;
            }
        }

        @Override
        public Iterator<? extends KBHandle> getChildren(KBHandle aNode)
        {
            try {
                // If the KB is read-only, then we cache the values and re-use the cached values.
                if (kbModel.getObject().isReadOnly()) {
                    List<KBHandle> children = childrensCache.get(aNode);
                    if (children == null) {
                        children = kbService.listChildConcepts(kbModel.getObject(),
                                aNode.getIdentifier(), preferences.getObject().showAllConcepts);
                        childrensCache.put(aNode, children);
                    }
                    return children.iterator();
                }
                else {
                    return kbService.listChildConcepts(kbModel.getObject(), aNode.getIdentifier(),
                            preferences.getObject().showAllConcepts).iterator();
                }
            }
            catch (QueryEvaluationException e) {
                error(getString("listChildConceptsErrorMsg") + ": " + e.getLocalizedMessage());
                LOG.error("Unable to list child concepts.", e);
                return Collections.emptyIterator();
            }
        }

        @Override
        public IModel<KBHandle> model(KBHandle aObject)
        {
            return Model.of(aObject);
        }
    }
    
    static class Preferences implements Serializable {
        private static final long serialVersionUID = 8310379405075949753L;

        boolean showAllConcepts;
    }
}
