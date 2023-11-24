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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.isFromImplicitNamespace;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.ConceptTreeProvider;
import de.tudarmstadt.ukp.inception.ui.kb.ConceptTreeProviderOptions;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class ConceptTreeBrowser
    extends Panel
{
    private static final long serialVersionUID = 8092655573891851744L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KBObject> concept;
    private IModel<KnowledgeBase> kbModel;
    private IModel<ConceptTreeProviderOptions> options;
    private IModel<Boolean> conceptNavigationEnabled;
    private IModel<Boolean> conceptSelectionEnabled;

    public ConceptTreeBrowser(String aId, IModel<KnowledgeBase> aKbModel, IModel<KBObject> aConcept)
    {
        super(aId, aConcept);

        setOutputMarkupId(true);

        concept = aConcept;
        kbModel = aKbModel;
        options = Model.of(new ConceptTreeProviderOptions());
        conceptNavigationEnabled = Model.of(true);
        conceptSelectionEnabled = Model.of(true);

        var tree = new DefaultNestedTree<KBObject>("tree",
                new ConceptTreeProvider(kbService, kbModel, options), Model.ofSet(new HashSet<>()))
        {
            private static final long serialVersionUID = -270550186750480253L;

            @Override
            protected Component newContentComponent(String id, IModel<KBObject> node)
            {
                return new ConceptTreeBrowserNode(id, this, node, concept, conceptNavigationEnabled,
                        conceptSelectionEnabled);
            }
        };
        add(tree);

        // Try expanding the path to the selected concept
        if (concept.isPresent().getObject()) {
            var c = (KBObject) concept.getObject();
            List<KBHandle> parents = kbService.getParentConceptList(c.getKB(), c.getIdentifier(),
                    false);
            LOG.debug("Trying to expand {}", parents);
            for (var h : parents) {
                tree.expand(h);
            }
        }

        Form<ConceptTreeProviderOptions> form = new Form<>("form",
                CompoundPropertyModel.of(options));
        form.add(new CheckBox("showAllConcepts") //
                .setOutputMarkupId(true) //
                .add(new LambdaAjaxFormSubmittingBehavior("change",
                        this::actionPreferenceChanged)));
        add(form);
    }

    public void setConceptNavigationEnabled(boolean aValue)
    {
        conceptNavigationEnabled.setObject(aValue);
    }

    public void setConceptSelectionEnabled(boolean aValue)
    {
        conceptSelectionEnabled.setObject(aValue);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(OnDomReadyHeaderItem.forScript("document.querySelector('#" + getMarkupId()
                + " .selected')?.scrollIntoView({block: 'center'});"));
    }

    /**
     * If the user disabled "show all" but a concept from an implicit namespace was selected, the
     * concept selection is cancelled. In any other case this component is merely updated via AJAX.
     */
    private void actionPreferenceChanged(AjaxRequestTarget aTarget)
    {
        if (!options.getObject().isShowAllConcepts() && concept.getObject() != null
                && isFromImplicitNamespace(concept.getObject())) {
            send(this, BUBBLE, new AjaxConceptSelectionEvent(aTarget, null, true));
        }
        else {
            aTarget.add(this);
        }
    }
}
