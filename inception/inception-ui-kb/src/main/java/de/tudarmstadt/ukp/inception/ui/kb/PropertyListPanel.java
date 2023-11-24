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
import java.util.ArrayList;
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
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewPropertyEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;

public class PropertyListPanel
    extends Panel
{

    private static final long serialVersionUID = 4129861816335804882L;
    private static final Logger LOG = LoggerFactory.getLogger(PropertyListPanel.class);

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KBProperty> selectedProperty;
    private IModel<KnowledgeBase> kbModel;
    private IModel<Preferences> preferences;

    public PropertyListPanel(String aId, IModel<KnowledgeBase> aKbModel, IModel<KBProperty> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        selectedProperty = aModel;
        kbModel = aKbModel;
        preferences = Model.of(new Preferences());

        OverviewListChoice<KBProperty> overviewList = new OverviewListChoice<>("properties");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
        overviewList.setModel(selectedProperty);
        overviewList.setChoices(LoadableDetachableModel.of(this::getProperties));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                this::actionSelectionChanged));

        add(overviewList);

        add(new Label("count", overviewList.getChoicesModel().map(Collection::size)));

        LambdaAjaxLink addLink = new LambdaAjaxLink("add",
                target -> send(getPage(), Broadcast.BREADTH, new AjaxNewPropertyEvent(target)));
        addLink.add(new Label("label", new ResourceModel("property.list.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(preferences));
        form.add(new CheckBox("showAllProperties").add(
                new LambdaAjaxFormSubmittingBehavior("change", this::actionPreferenceChanged)));
        add(form);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // Via Wicket events it can happen that an explicit property is selected even though the
        // preferences filtered implicit statements from the list. It is necessary to flip the
        // filter preferences in this case to keep the UI consistent.
        boolean isSelectedPropertyFromImplicitNamespace = selectedProperty.getObject() != null
                && IriConstants.isFromImplicitNamespace(selectedProperty.getObject());
        if (isSelectedPropertyFromImplicitNamespace) {
            preferences.getObject().showAllProperties = true;
        }
    }

    private void actionSelectionChanged(AjaxRequestTarget aTarget)
    {
        // if the selection changes, publish an event denoting the change
        AjaxPropertySelectionEvent e = new AjaxPropertySelectionEvent(aTarget,
                selectedProperty.getObject());
        send(getPage(), Broadcast.BREADTH, e);
    }

    /**
     * If the user disabled "show all" but a property from an implicit namespace was selected, the
     * property selection is cancelled. In any other case this component is merely updated via AJAX.
     */
    private void actionPreferenceChanged(AjaxRequestTarget aTarget)
    {
        if (!preferences.getObject().showAllProperties && selectedProperty.getObject() != null
                && IriConstants.isFromImplicitNamespace(selectedProperty.getObject())) {
            send(getPage(), Broadcast.BREADTH, new AjaxPropertySelectionEvent(aTarget, null, true));
        }
        else {
            aTarget.add(this);
        }
    }

    private List<KBProperty> getProperties()
    {
        if (isVisibleInHierarchy()) {
            Preferences prefs = preferences.getObject();
            List<KBProperty> statements = new ArrayList<>();
            try {
                statements = kbService.listProperties(kbModel.getObject(), prefs.showAllProperties);
                return statements;
            }
            catch (QueryEvaluationException e) {
                // FIXME when this error(...) is called, a
                // -org.apache.wicket.WicketRuntimeException:
                // Cannot modify component hierarchy after render phase has started- is thrown.
                // error("Unable to list properties: " + e.getLocalizedMessage());
                LOG.debug("Unable to list properties.", e);
                KBProperty errorPlaceholder = new KBProperty();
                errorPlaceholder.setName("Unable to list properties.");
                statements.add(errorPlaceholder);
                return statements;
            }
        }
        else {
            return Collections.emptyList();
        }
    }

    static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = 4181477265434386379L;

        boolean showAllProperties;
    }
}
