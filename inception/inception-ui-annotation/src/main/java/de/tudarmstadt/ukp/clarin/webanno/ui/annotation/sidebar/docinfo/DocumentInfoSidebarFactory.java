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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docinfo;

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
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

// This now mainly serves as example code - we do not actually use it
@Order(1)
public class DocumentInfoSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    @Override
    public String getDisplayName()
    {
        return "Document Info";
    }

    @Override
    public String getDescription()
    {
        return "Displays basic information about the current document.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorState> aState)
    {
        return new Icon(aId, FontAwesome5IconType.info_s);
    }

    @Override
    public boolean available(Project aProject)
    {
        return false;
    }

    @Override
    public boolean applies(AnnotatorState aState)
    {
        return false;
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        return new DocumentInfoSidebar(aId, aActionHandler, aCasProvider, aAnnotationPage);
    }
}
