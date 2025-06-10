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
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

public class SidebarPanel
    extends Panel
{
    private static final long serialVersionUID = 5654603956968658069L;

    private @SpringBean AnnotationSidebarRegistry sidebarRegistry;

    private AnnotationActionHandler actionHandler;
    private CasProvider casProvider;
    private AnnotationPageBase2 annotationPage;
    private SidebarTabbedPanel<SidebarTab> tabsPanel;

    public SidebarPanel(String aId, IModel<Integer> aWidthModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider,
            AnnotationPageBase2 aAnnotationPage)
    {
        super(aId);

        Validate.notNull(aActionHandler, "Action handler must not be null");

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        actionHandler = aActionHandler;
        casProvider = aCasProvider;
        annotationPage = aAnnotationPage;

        tabsPanel = new SidebarTabbedPanel<>("leftSidebarContent", makeTabs(),
                annotationPage.getModel());
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
        setVisible(annotationPage.getModel() //
                .map(state -> state.getDocument() != null) //
                .orElse(false).getObject());
    }

    public void refreshTabs(AjaxRequestTarget aTarget)
    {
        // re-init tabs list with valid tabs
        var tabs = tabsPanel.getTabs();
        tabs.clear();
        tabs.addAll(makeTabs());
        aTarget.add(tabsPanel);
    }

    public void showTab(AjaxRequestTarget aTarget, String aFactoryId)
    {
        tabsPanel.getTabs().stream() //
                .filter(tab -> tab.getFactoryId().equals(aFactoryId)) //
                .findFirst() //
                .ifPresent(tab -> tabsPanel.setSelectedTab(tabsPanel.getTabs().indexOf(tab)));
        aTarget.add(tabsPanel);
    }

    private List<SidebarTab> makeTabs()
    {
        var tabs = new ArrayList<SidebarTab>();
        for (var factory : sidebarRegistry.getExtensions()) {

            if (!factory.accepts(annotationPage)) {
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
                                .map($ -> (Panel) $.create(aId, actionHandler, casProvider,
                                        annotationPage))
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
