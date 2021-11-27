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

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamEditorAutoConfig;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamEditorAutoConfig#extensionActionHandler}.
 * </p>
 */
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
    public void handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        AnnotationPageBase page = (AnnotationPageBase) aTarget.getPage();

        // Get action from the request
        String action = getAction(aRequest);

        // Parse annotation ID if present in request
        final VID paramId = getVid(aRequest);

        try {
            CAS cas = page.getEditorCas();

            extensionRegistry.fireAction(page.getAnnotationActionHandler(), page.getModelObject(),
                    aTarget, cas, paramId, action);
        }
        catch (Exception e) {
            handleError(aTarget, "Unable to load data", e);
        }
    }
}
