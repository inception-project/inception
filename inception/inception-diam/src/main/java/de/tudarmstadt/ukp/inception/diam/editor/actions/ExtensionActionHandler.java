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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#extensionActionHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_EXTENSION_HANDLER)
public class ExtensionActionHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "doAction";

    private final AnnotationEditorExtensionRegistry extensionRegistry;

    public ExtensionActionHandler(AnnotationEditorExtensionRegistry aExtensionRegistry)
    {
        extensionRegistry = aExtensionRegistry;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public boolean accepts(Request aRequest)
    {
        return getVid(aRequest).isSynthetic();
    }

    @Override
    public DefaultAjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var page = getPage();
            page.ensureIsEditable();

            var action = getAction(aRequest);
            var paramId = getVid(aRequest);
            var cas = page.getEditorCas();

            extensionRegistry.fireAction(page.getAnnotationActionHandler(), page.getModelObject(),
                    aTarget, cas, paramId, action);

            return new DefaultAjaxResponse(action);
        }
        catch (Exception e) {
            return handleError("Unable to perform action", e);
        }
    }
}
