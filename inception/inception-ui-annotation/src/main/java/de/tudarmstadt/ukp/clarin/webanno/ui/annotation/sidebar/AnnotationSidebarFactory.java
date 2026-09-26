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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface AnnotationSidebarFactory
    extends Extension<SidebarContext>
{
    /**
     * @return get the bean name.
     */
    String getBeanName();

    String getDisplayName();

    String getDescription();

    Component createIcon(String aId, IModel<AnnotatorViewState> aState);

    AnnotationSidebar_ImplBase create(String id, SidebarContext aContext);

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
     * @return if the sidebar applies in the given context. Override for cases when the sidebar
     *         should not be added by default.
     */
    @Override
    default boolean accepts(SidebarContext aContext)
    {
        var project = aContext.getProject();

        // available() implementations typically run a database query, so do not hand them a null
        // project. No project means there is nothing to decide about.
        return project != null && available(project);
    }
}
