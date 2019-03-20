/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;

public class SidebarPanel
    extends Panel
{
    private static final long serialVersionUID = 5654603956968658069L;

    private @SpringBean AnnotationSidebarRegistry sidebarRegistry;

    private AnnotationActionHandler actionHandler;
    private JCasProvider jCasProvider;
    private AnnotationPage annotationPage;
    private IModel<AnnotatorState> stateModel;
    
    public SidebarPanel(String aId, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId);

        Validate.notNull(aActionHandler, "Action handler must not be null");
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        actionHandler = aActionHandler;
        jCasProvider = aJCasProvider;
        annotationPage = aAnnotationPage;
        stateModel = aModel;
        
        SidebarTabbedPanel<SidebarTab> tabsPanel = new SidebarTabbedPanel<>("leftSidebarContent",
                makeTabs());
        add(tabsPanel);
        
        add(new AttributeAppender("class", () -> tabsPanel.isExpanded() ? "" : "collapsed", " "));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        // Only show sidebar if a document is selected
        setVisible(stateModel.getObject() != null && stateModel.getObject().getDocument() != null);
    }
        
    private List<SidebarTab> makeTabs()
    {
        List<SidebarTab> tabs = new ArrayList<>();
        for (AnnotationSidebarFactory factory : sidebarRegistry.getSidebarFactories()) {
            String factoryId = factory.getBeanName();
            SidebarTab tab = new SidebarTab(Model.of(factory.getDisplayName()), factory.getIcon())
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
                                        actionHandler, jCasProvider, annotationPage);
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
