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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class SidebarPanel
    extends Panel
{
    private static final long serialVersionUID = 5654603956968658069L;

    private @SpringBean AnnotationSidebarRegistry sidebarRegistry;

    private AnnotationActionHandler actionHandler;
    private CasProvider casProvider;
    private AnnotationPage annotationPage;
    private IModel<AnnotatorState> stateModel;
    private SidebarTabbedPanel<SidebarTab> tabsPanel;

    public SidebarPanel(String aId, IModel<AnnotatorState> aModel, IModel<Integer> aWidthModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId);

        Validate.notNull(aActionHandler, "Action handler must not be null");

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        actionHandler = aActionHandler;
        casProvider = aCasProvider;
        annotationPage = aAnnotationPage;
        stateModel = aModel;

        tabsPanel = new SidebarTabbedPanel<>("leftSidebarContent", makeTabs(), stateModel);
        add(tabsPanel);

        add(new AttributeAppender("class",
                LoadableDetachableModel.of(() -> tabsPanel.isExpanded() ? "" : "collapsed"), " "));

        // Override sidebar width from preferences
        add(new AttributeModifier("style",
                LoadableDetachableModel.of(() -> tabsPanel.isExpanded()
                        ? format("flex-basis: %d%%;", aWidthModel.orElse(20).getObject())
                        : "")));
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // Only show sidebar if a document is selected
        setVisible(stateModel.getObject() != null && stateModel.getObject().getDocument() != null);
    }

    public void refreshTabs(AjaxRequestTarget aTarget)
    {
        // re-init tabs list with valid tabs
        List<SidebarTab> tabs = tabsPanel.getTabs();
        tabs.clear();
        tabs.addAll(makeTabs());
        aTarget.add(tabsPanel);
    }

    private List<SidebarTab> makeTabs()
    {
        List<SidebarTab> tabs = new ArrayList<>();
        for (AnnotationSidebarFactory factory : sidebarRegistry.getSidebarFactories()) {

            if (!factory.applies(stateModel.getObject())) {
                continue;
            }

            String factoryId = factory.getBeanName();
            SidebarTab tab = new SidebarTab(Model.of(factory.getDisplayName()), factory.getIcon(),
                    factory.getBeanName())
            {
                private static final long serialVersionUID = 2144644282070158783L;

                @Override
                public Panel getPanel(String aPanelId)
                {
                    try {
                        // We need to get the methods and services directly in here so
                        // that the lambda doesn't have a dependency on the non-serializable
                        // AnnotationSidebarFactory class.
                        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                        return ctx.getBean(AnnotationSidebarRegistry.class)
                                .getSidebarFactory(factoryId).create(aPanelId, stateModel,
                                        actionHandler, casProvider, annotationPage);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            tabs.add(tab);
        }
        return tabs;
    }
}
