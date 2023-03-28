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

import static de.tudarmstadt.ukp.inception.ui.kb.feature.InstanceListSortKeys.LABEL;
import static org.apache.wicket.event.Broadcast.BUBBLE;
import static org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;

public class InstanceListBrowser
    extends Panel
{
    private static final long serialVersionUID = 5630696070130423532L;

    private static final String CID_LABEL = "label";
    private static final String CID_DESCRIPTION = "description";
    private static final String CID_INSTANCES = "instances";

    private @SpringBean KnowledgeBaseService kbService;

    private final InstanceListDataProvider dataProvider;
    private final IModel<KBObject> concept;
    private final IModel<List<? extends KBObject>> instances;

    public InstanceListBrowser(String aId, IModel<KBObject> aConcept, IModel<KBObject> aInstance)
    {
        super(aId, aInstance);

        concept = aConcept;

        instances = LoadableDetachableModel.of(this::loadInstances);

        dataProvider = new InstanceListDataProvider(instances);
        dataProvider.setSort(LABEL, ASCENDING);

        queue(createInstanceList(CID_INSTANCES, dataProvider));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(OnDomReadyHeaderItem.forScript("document.querySelector('#" + getMarkupId()
                + " .border-primary')?.scrollIntoView({block: 'center'});"));
    }

    @SuppressWarnings("unchecked")
    public IModel<KBObject> getModel()
    {
        return (IModel<KBObject>) getDefaultModel();
    }

    public KBObject getModelObject()
    {
        return (KBObject) getDefaultModelObject();
    }

    public void refresh()
    {
        instances.detach();
        dataProvider.refresh();
    }

    private List<? extends KBObject> loadInstances()
    {
        return concept //
                .map(c -> kbService.listInstances(c.getKB(), c.getIdentifier(), false)) //
                .orElse(Collections.emptyList()).getObject();
    }

    private DataView<KBObject> createInstanceList(String aId,
            InstanceListDataProvider aDataProvider)
    {
        return new DataView<KBObject>(aId, aDataProvider, 25)
        {
            private static final long serialVersionUID = -755155675319764642L;

            @Override
            protected void populateItem(Item<KBObject> aItem)
            {
                if (getModelObject() != null && aItem.getModelObject().getIdentifier()
                        .equals(getModelObject().getIdentifier())) {
                    aItem.add(AttributeAppender.append("class", "border border-3 border-primary"));
                }
                aItem.add(new Label(CID_LABEL, aItem.getModelObject().getUiLabel()));
                aItem.add(new Label(CID_DESCRIPTION, aItem.getModelObject().getDescription()));
                aItem.add(AjaxEventBehavior.onEvent("click",
                        _target -> actionSelectInstance(_target, aItem)));
            }
        };
    }

    private void actionSelectInstance(AjaxRequestTarget aTarget, Item<KBObject> aItem)
    {
        send(this, BUBBLE, new AjaxInstanceSelectionEvent(aTarget, aItem.getModelObject()));
    }
}
