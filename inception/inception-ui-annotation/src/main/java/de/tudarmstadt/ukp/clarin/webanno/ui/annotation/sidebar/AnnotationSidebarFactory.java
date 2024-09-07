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

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface AnnotationSidebarFactory
    extends Extension<AnnotationPageBase>
{
    /**
     * @return get the bean name.
     */
    String getBeanName();

    String getDisplayName();

    String getDescription();

    Component createIcon(String aId, IModel<AnnotatorState> aState);

    AnnotationSidebar_ImplBase create(String id, final AnnotationActionHandler aActionHandler,
            final CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage);

    /**
     * @return if the sidebar is available for the given project. Override for cases when sidebar
     *         should not be available by default
     */
    @SuppressWarnings("javadoc")
    default boolean available(Project aProject)
    {
        return true;
    }

    /**
     * @return if the sidebar applies to the given annotator state. Override for cases when sidebar
     *         should not be added by default
     * @deprecated to be removed in favor of {@link #accepts(AnnotationPageBase)}
     */
    @Deprecated
    @SuppressWarnings("javadoc")
    default boolean applies(AnnotatorState aState)
    {
        return true;
    }

    /**
     * @return if the sidebar applies to the given page state. Override for cases when sidebar
     *         should not be added by default
     */
    @Override
    default boolean accepts(AnnotationPageBase aContext)
    {
        return applies(aContext.getModelObject());
    }
}
