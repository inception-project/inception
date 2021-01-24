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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementDetailPreference;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.model.StatementGroupBean;

public class PropertyPanel
    extends Panel
{

    private static final long serialVersionUID = -6318957011849336285L;

    private static final Set<String> IMPORTANT_PROPERTY_URIS = new HashSet<>(
            Arrays.asList("http://www.w3.org/2000/01/rdf-schema#subPropertyOf",
                    "http://www.w3.org/2000/01/rdf-schema#domain",
                    "http://www.w3.org/2000/01/rdf-schema#range"));

    private @SpringBean KnowledgeBaseService kbService;

    private List<String> labelProperties;

    public PropertyPanel(String id, IModel<KnowledgeBase> aKbModel, IModel<KBProperty> handleModel,
            IModel<KBProperty> selectedPropertyModel)
    {
        super(id, selectedPropertyModel);
        add(new PropertyInfoPanel("info", aKbModel, handleModel, selectedPropertyModel));
    }

    private class PropertyInfoPanel
        extends AbstractInfoPanel<KBProperty>
    {

        private static final long serialVersionUID = -1413622323011843523L;

        public PropertyInfoPanel(String aId, IModel<KnowledgeBase> aKbModel,
                IModel<? extends KBObject> handleModel, IModel<KBProperty> aModel)
        {
            super(aId, aKbModel, handleModel, aModel);
        }

        @Override
        protected void actionCreate(AjaxRequestTarget aTarget, Form<KBProperty> aForm)
        {
            KBProperty prop = kbObjectModel.getObject();

            assert isEmpty(prop.getIdentifier());
            kbService.createProperty(kbModel.getObject(), prop);

            // select newly created property right away to show the statements
            send(getPage(), Broadcast.BREADTH, new AjaxPropertySelectionEvent(aTarget, prop, true));
        }

        @Override
        protected void actionDelete(AjaxRequestTarget aTarget)
        {
            kbService.deleteProperty(kbModel.getObject(), kbObjectModel.getObject());
            kbObjectModel.setObject(null);

            // send deselection event
            send(getPage(), Broadcast.BREADTH, new AjaxPropertySelectionEvent(aTarget, null, true));
        }

        @Override
        protected void actionCancel(AjaxRequestTarget aTarget)
        {
            kbObjectModel.setObject(null);

            // send deselection event
            send(getPage(), Broadcast.BREADTH, new AjaxPropertySelectionEvent(aTarget, null));
        }

        @Override
        public List<String> getLabelProperties()
        {
            if (labelProperties == null) {
                labelProperties = kbService.listPropertyLabelProperties(kbModel.getObject());
            }

            return labelProperties;
        }

        @Override
        protected Comparator<StatementGroupBean> getStatementGroupComparator()
        {
            return new ImportantStatementComparator<>(sgb -> sgb.getProperty().getIdentifier(),
                    identifier -> IMPORTANT_PROPERTY_URIS.contains(identifier)
                            || kbService.isBaseProperty(identifier, kbModel.getObject())
                            || getLabelProperties().contains(identifier));
        }

        @Override
        protected String getTypeLabelResourceKey()
        {
            return "property";
        }

        @Override
        protected String getNamePlaceholderResourceKey()
        {
            return "property.new.placeholder";
        }

        @Override
        protected StatementDetailPreference getDetailPreference()
        {
            return StatementDetailPreference.ALL;
        }
    }
}
