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

import static de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider.getApplicationContext;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.ShowAnnotationReadOnlyEvent;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemContext;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemExtension;
import de.tudarmstadt.ukp.inception.curation.api.CurationVID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;

public class ShowSuggestionDetailsContextMenuItem
    implements ContextMenuItemExtension
{
    private final CurationEditorExtension curationEditorExtension;
    private final UserDao userService;

    public ShowSuggestionDetailsContextMenuItem(CurationEditorExtension aCurationEditorExtension,
            UserDao aUserService)
    {
        curationEditorExtension = aCurationEditorExtension;
        userService = aUserService;
    }

    @Override
    public boolean accepts(ContextMenuItemContext aCtx)
    {
        var vid = aCtx.vid();
        return vid.isSynthetic() //
                && CurationEditorExtension.EXTENSION_ID.equals(vid.getExtensionId())
                && CurationVID.parse(vid.getExtensionPayload()) != null;
    }

    @Override
    public IMenuItem createMenuItem(ContextMenuItemContext aCtx, int aClientX, int aClientY)
    {
        return new LambdaMenuItem("Show details", $ -> {
            // Ensure that lambda is serializable
            getApplicationContext() //
                    .getBean(ShowSuggestionDetailsContextMenuItem.class) //
                    .actionShowDetails($, aCtx);
        });
    }

    private void actionShowDetails(AjaxRequestTarget aTarget, ContextMenuItemContext aCtx)
        throws IOException
    {
        var context = aCtx.context();
        var state = context.getAnnotatorState();
        var curationVid = CurationVID.parse(aCtx.vid().getExtensionPayload());
        var vid = VID.parse(curationVid.getExtensionPayload());
        if (vid.isSlotSet()) {
            // For a slot, show the slot host
            vid = new VID(vid.getId());
        }

        var dataOwner = userService.get(curationVid.getUsername());
        var otherDataOwners = curationEditorExtension
                .listSuggestionAnnotators(state.getDocument(), state.getUser(),
                        context.getEditorCas(), aCtx.vid())
                .stream() //
                .filter(username -> !username.equals(curationVid.getUsername())) //
                .map(userService::get) //
                .filter(Objects::nonNull) //
                .toList();

        var page = aTarget.getPage();
        page.send(page, BREADTH, new ShowAnnotationReadOnlyEvent(aTarget, state.getDocument(),
                dataOwner, vid, otherDataOwners));
    }
}
