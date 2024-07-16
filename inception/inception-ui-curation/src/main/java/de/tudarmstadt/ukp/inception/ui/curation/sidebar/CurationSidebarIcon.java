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

import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType.clipboard_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType.play_circle_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType.stop_circle_s;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class CurationSidebarIcon
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = -1870047500327624860L;

    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userService;

    public CurationSidebarIcon(String aId, IModel<AnnotatorState> aState)
    {
        super(aId, aState);

        queue(new Icon("icon", clipboard_s));
        queue(new Icon("badge", () -> isSessionActive() ? play_circle_s : stop_circle_s) //
                .add(LambdaBehavior.visibleWhen(this::isSidebarCurationMode)) //
                .add(AttributeModifier.append("class",
                        () -> isSessionActive() ? "text-primary" : "text-muted")));
    }

    private boolean isSessionActive()
    {
        var project = getModelObject().getProject();

        if (project != null && curationSidebarService
                .existsSession(userService.getCurrentUsername(), project.getId())) {
            return true;
        }

        return false;
    }

    private boolean isSidebarCurationMode()
    {
        return getPage() instanceof AnnotationPage;
    }
}
