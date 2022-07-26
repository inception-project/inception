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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.sidebar;

import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.config.ExternalSearchUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * Sidebar to access the external search on the annotation page.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchUIAutoConfiguration#externalSearchAnnotationSidebarFactory}.
 * </p>
 */
public class ExternalSearchAnnotationSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private final ExternalSearchService externalSearchService;

    public ExternalSearchAnnotationSidebarFactory(ExternalSearchService aExternalSearchService)
    {
        externalSearchService = aExternalSearchService;
    }

    @Override
    public String getDisplayName()
    {
        return "External Search";
    }

    @Override
    public String getDescription()
    {
        return "Allows searching document repositories and importing documents. Only available if "
                + "there are document repositories defined in the project.";
    }

    @Override
    public IconType getIcon()
    {
        return FontAwesome5IconType.database_s;
    }

    @Override
    public boolean available(Project aProject)
    {
        return externalSearchService.existsEnabledDocumentRepository(aProject);
    }

    @Override
    public boolean applies(AnnotatorState aState)
    {
        return available(aState.getProject());
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        return new ExternalSearchAnnotationSidebar(aId, aModel, aActionHandler, aCasProvider,
                aAnnotationPage);
    }
}
