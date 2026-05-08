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
package de.tudarmstadt.ukp.inception.assistant.contextmenu;

import static de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider.getApplicationContext;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemContext;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemExtension;
import de.tudarmstadt.ukp.inception.assistant.sidebar.AssistantSidebarFactory;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;

public class CheckAnnotationContextMenuItem
    implements ContextMenuItemExtension
{
    private final SchedulingService schedulingService;
    private final AssistantSidebarFactory assistantSidebarFactory;
    private final AnnotationSchemaService schemaService;
    private final UserDao userService;

    public CheckAnnotationContextMenuItem(SchedulingService aSchedulingService,
            AssistantSidebarFactory aAssistantSidebarFactory,
            AnnotationSchemaService aSchemaService, UserDao aUserService)
    {
        schedulingService = aSchedulingService;
        assistantSidebarFactory = aAssistantSidebarFactory;
        schemaService = aSchemaService;
        userService = aUserService;
    }

    @Override
    public boolean accepts(ContextMenuItemContext aCtx)
    {
        var state = aCtx.page().getModelObject();

        try {
            var cas = aCtx.page().getEditorCas();
            var ann = selectAnnotationByAddr(cas, aCtx.vid().getId());
            if (ann == null) {
                return false;
            }

            return SpanLayerSupport.TYPE
                    .equals(schemaService.findLayer(state.getProject(), ann).getType());
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public IMenuItem createMenuItem(VID aVid, int aClientX, int aClientY)
    {
        return new LambdaMenuItem("Check ...", "fa-regular fa-comments", $ -> {
            // Need to fetch the item from the application context statically to make the
            // lambda
            // serializable
            getApplicationContext() //
                    .getBean(CheckAnnotationContextMenuItem.class) //
                    .actionCheck($, aVid, aClientX, aClientY);
        });
    }

    private void actionCheck(AjaxRequestTarget aTarget, VID paramId, int aClientX, int aClientY)
        throws IOException
    {
        var page = (AnnotationPageBase) aTarget.getPage();

        var maybeContextMenuLookup = page.getContextMenuLookup();
        if (!maybeContextMenuLookup.isPresent()) {
            return;
        }

        page.visitChildren(SidebarPanel.class,
                (c, v) -> ((SidebarPanel) c).showTab(aTarget, assistantSidebarFactory.getId()));

        var sessionOwner = userService.getCurrentUser();
        var state = page.getModelObject();
        schedulingService.enqueue(CheckAnnotationTask.builder() //
                .withTrigger("Context menu") //
                .withSessionOwner(sessionOwner) //
                .withProject(state.getProject()) //
                .withDocument(state.getDocument()) //
                .withDataOwner(state.getUser().getUsername()) //
                .withAnnotation(paramId) //
                .build());
    }
}
