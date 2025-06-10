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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationSidebarFactory}.
 * </p>
 */
@Order(3000)
public class CurationSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final ProjectService projectService;
    private final UserDao userService;
    private final CurationSidebarProperties curationSidebarProperties;

    public CurationSidebarFactory(ProjectService aProjectService, UserDao aUserService,
            CurationSidebarProperties aCurationSidebarProperties)
    {
        projectService = aProjectService;
        userService = aUserService;
        curationSidebarProperties = aCurationSidebarProperties;
    }

    @Override
    public String getDisplayName()
    {
        return "Curation";
    }

    @Override
    public String getDescription()
    {
        return "Allows curation via the annotation page. Only available to curators.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorState> aState)
    {
        return new CurationSidebarIcon(aId, aState);
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        return new CurationSidebar(aId, aActionHandler, aCasProvider, aAnnotationPage);
    }

    @Override
    public boolean accepts(AnnotationPageBase aContext)
    {
        if ((aContext instanceof AnnotationPage && curationSidebarProperties.isEnabled())
                || aContext instanceof CurationPage) {
            var state = aContext.getModelObject();
            var sessionOwner = userService.getCurrentUsername();
            var isCurator = projectService.hasRole(state.getUser(), state.getProject(), CURATOR);
            return isCurator && state.getUser().getUsername().equals(sessionOwner);
        }

        return false;
    }
}
