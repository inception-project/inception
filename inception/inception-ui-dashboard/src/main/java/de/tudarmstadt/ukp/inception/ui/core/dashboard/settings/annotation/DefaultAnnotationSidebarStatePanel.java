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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarTabbedPanel.KEY_SIDEBAR_STATE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class DefaultAnnotationSidebarStatePanel
    extends Panel
{
    private static final long serialVersionUID = -8186980821674705506L;

    private @SpringBean AnnotationSidebarRegistry annotationSidebarRegistry;
    private @SpringBean PreferencesService preferencesService;

    private IModel<SidebarHandle> defaultTab;

    public DefaultAnnotationSidebarStatePanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        defaultTab = Model.of();

        var form = new LambdaForm<Void>("form");

        var description = new Label("description", defaultTab.map(SidebarHandle::getDescription));
        description.setOutputMarkupPlaceholderTag(true);
        description.add(visibleWhen(defaultTab.isPresent()));
        form.add(description);

        var defaultTabSelect = new DropDownChoice<SidebarHandle>("defaultTab", defaultTab,
                LoadableDetachableModel.of(this::listAvailableSidebars),
                new ChoiceRenderer<>("displayName", "id"));
        defaultTabSelect.setNullValid(true);
        defaultTabSelect.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> _target.add(description)));
        form.add(defaultTabSelect);

        form.onSubmit(this::actionSave);

        add(form);

        actionLoad();
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private void actionLoad()
    {
        var state = preferencesService.loadDefaultTraitsForProject(KEY_SIDEBAR_STATE,
                getModel().getObject());

        var factory = annotationSidebarRegistry.getExtension(state.getSelectedTab());
        defaultTab.setObject(factory.map(SidebarHandle::new).orElse(null));
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aDummy)
    {
        var state = preferencesService.loadDefaultTraitsForProject(KEY_SIDEBAR_STATE,
                getModel().getObject());

        state.setSelectedTab(defaultTab.map(SidebarHandle::getId).orElse(null).getObject());
        state.setExpanded(defaultTab.isPresent().getObject());

        preferencesService.saveDefaultTraitsForProject(KEY_SIDEBAR_STATE, getModel().getObject(),
                state);
    }

    private List<SidebarHandle> listAvailableSidebars()
    {
        return annotationSidebarRegistry.getExtensions().stream() //
                .map(SidebarHandle::new) //
                .collect(toList());
    }

    private class SidebarHandle
        implements Serializable
    {
        private static final long serialVersionUID = 5906057376121692416L;

        private String id;
        private String displayName;
        private String description;

        public SidebarHandle(AnnotationSidebarFactory aFactory)
        {
            id = aFactory.getBeanName();
            displayName = aFactory.getDisplayName();
            if (!aFactory.available(getModel().getObject())) {
                displayName += " (not available)";
            }
            description = aFactory.getDescription();
        }

        public String getId()
        {
            return id;
        }

        @SuppressWarnings("unused")
        public String getDisplayName()
        {
            return displayName;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
