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
package de.tudarmstadt.ukp.inception.assistant.sidebar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.assistant.AssistantService;
import de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class AssistantSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -1585047099720374119L;

    private @SpringBean UserDao userService;
    private @SpringBean AssistantService assistantService;
    private @SpringBean DocumentQueryService documentQueryService;
    
    private AssistantPanel chat;

    public AssistantSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        chat = new AssistantPanel("chat");
        queue(chat);

        queue(new LambdaAjaxLink("reindex", this::actionReindex));

        queue(new LambdaAjaxLink("clear", this::actionClear));
}

    private void actionReindex(AjaxRequestTarget aTarget)
    {
        documentQueryService.rebuildIndexAsync(getAnnotationPage().getProject());
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userService.getCurrentUsername();
        var project = getAnnotationPage().getProject();
        assistantService.clearConversation(sessionOwner, project);
        // FIXME instead of reloading the chat, we should send a clearing message in the assistant service
        aTarget.add(chat);
    }
}
