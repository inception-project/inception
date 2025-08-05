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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DATA_OWNER;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static java.lang.String.format;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.config.AnnotationUIAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;
import wicket.contrib.input.events.key.KeyType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationUIAutoConfiguration#annotationPageMenuItem}.
 * </p>
 */
@Order(100)
public class AnnotationPageMenuItem
    implements ProjectMenuItem
{
    private final UserDao userRepo;
    private final ProjectService projectService;
    private final ServletContext servletContext;

    public AnnotationPageMenuItem(UserDao aUserRepo, ProjectService aProjectService,
            ServletContext aServletContext)
    {
        userRepo = aUserRepo;
        projectService = aProjectService;
        servletContext = aServletContext;
    }

    @Override
    public String getPath()
    {
        return "/annotate";
    }

    public String getUrl(Project aProject, long aDocumentId)
    {
        var p = aProject.getSlug() != null ? aProject.getSlug() : String.valueOf(aProject.getId());

        return format("%s/p/%s%s/%d", servletContext.getContextPath(), p, getPath(), aDocumentId);
    }

    public String getUrl(Project aProject, long aDocumentId, String aDataOwner)
    {
        var p = aProject.getSlug() != null ? aProject.getSlug() : String.valueOf(aProject.getId());

        return format("%s/p/%s%s/%d?%s=%s", servletContext.getContextPath(), p, getPath(),
                aDocumentId, PAGE_PARAM_DATA_OWNER, aDataOwner);
    }

    @Override
    public Component getIcon(String aId)
    {
        return new Icon(aId, FontAwesome5IconType.highlighter_s);
    }

    @Override
    public String getLabel()
    {
        return "Annotation";
    }

    /**
     * Only project admins, annotators and curators can see this page
     */
    @Override
    public boolean applies(Project aProject)
    {
        if (aProject == null) {
            return false;
        }

        // Visible if the current user is an annotator
        var sessionOwner = userRepo.getCurrentUser();
        return projectService.hasRole(sessionOwner, aProject, ANNOTATOR, CURATOR);
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return AnnotationPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.a };
    }
}
