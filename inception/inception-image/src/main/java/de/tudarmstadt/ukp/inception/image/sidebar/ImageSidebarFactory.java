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
package de.tudarmstadt.ukp.inception.image.sidebar;

import static de.tudarmstadt.ukp.inception.image.feature.ImageFeatureSupport.TYPE_IMAGE_URL;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.image.config.ImageSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ImageSupportAutoConfiguration#imageSidebarFactory}.
 * </p>
 */
@Order(10000)
public class ImageSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private final AnnotationSchemaService schemaService;

    public ImageSidebarFactory(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public String getDisplayName()
    {
        return "Images";
    }

    @Override
    public String getDescription()
    {
        return "Displays images linked to annotations that are currently visible on screen. Only "
                + "available if the project defines at least one image feature on any layer.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorState> aState)
    {
        return new Icon(aId, FontAwesome5IconType.images_s);
    }

    @Override
    public boolean available(Project aProject)
    {
        return schemaService.existsEnabledFeatureOfType(aProject, TYPE_IMAGE_URL);
    }

    @Override
    public boolean applies(AnnotatorState aState)
    {
        return available(aState.getProject());
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        return new ImageSidebar(aId, aActionHandler, aCasProvider, aAnnotationPage);
    }
}
