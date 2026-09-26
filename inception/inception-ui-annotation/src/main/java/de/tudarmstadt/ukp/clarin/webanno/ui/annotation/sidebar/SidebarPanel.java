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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

public class SidebarPanel
    extends Panel
{
    private static final long serialVersionUID = 5654603956968658069L;

    private @SpringBean AnnotationSidebarRegistry sidebarRegistry;

    private AnnotationPageBase2 annotationPage;
    private SidebarTabbedPanel<SidebarTab> tabsPanel;

    public SidebarPanel(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId);

        Validate.notNull(aAnnotationPage, "Annotation page must not be null");

        setOutputMarkupPlaceholderTag(true);

        annotationPage = aAnnotationPage;

        tabsPanel = new SidebarTabbedPanel<>("leftSidebarContent", makeTabs(), annotationPage,
                LoadableDetachableModel.of(() -> annotationPage.getWorkspace().getActiveEditor() //
                        .map(DiamContext::getAnnotatorState) //
                        .orElse(null)));
        add(tabsPanel);

        add(new AttributeAppender("class",
                LoadableDetachableModel.of(() -> tabsPanel.isExpanded() ? "" : "collapsed"), " "));
    }

    public boolean isCollapsed()
    {
        return !tabsPanel.isExpanded();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(annotationPage.getWorkspace().hasOpenDocument());
    }

    public void refreshTabs(AjaxRequestTarget aTarget)
    {
        // re-init tabs list with valid tabs
        var tabs = tabsPanel.getTabs();
        tabs.clear();
        tabs.addAll(makeTabs());
        aTarget.add(this);
    }

    public void showTab(AjaxRequestTarget aTarget, String aFactoryId)
    {
        tabsPanel.getTabs().stream() //
                .filter(tab -> tab.getFactoryId().equals(aFactoryId)) //
                .findFirst() //
                .ifPresent(tab -> tabsPanel.setSelectedTab(tabsPanel.getTabs().indexOf(tab)));
        aTarget.add(tabsPanel);
    }

    private SidebarContext newContext()
    {
        return new SidebarContext(annotationPage, annotationPage.getWorkspace());
    }

    private List<SidebarTab> makeTabs()
    {
        var tabs = new ArrayList<SidebarTab>();
        for (var factory : sidebarRegistry.getExtensions()) {

            if (!factory.accepts(newContext())) {
                continue;
            }

            var factoryId = factory.getBeanName();
            var tab = new SidebarTab(Model.of(factory.getDisplayName()), factory.getBeanName())
            {
                private static final long serialVersionUID = 2144644282070158783L;

                @Override
                public Panel getPanel(String aId)
                {
                    try {
                        // We need to get the methods and services directly in here so
                        // that the lambda doesn't have a dependency on the non-serializable
                        // AnnotationSidebarFactory class.
                        var ctx = ApplicationContextProvider.getApplicationContext();
                        return ctx.getBean(AnnotationSidebarRegistry.class) //
                                .getExtension(factoryId) //
                                .map($ -> (Panel) $.create(aId, newContext()))
                                .orElseGet(() -> new EmptyPanel(aId));
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
