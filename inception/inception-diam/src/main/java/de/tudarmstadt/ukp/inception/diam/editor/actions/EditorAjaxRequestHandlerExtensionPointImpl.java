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

import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamAutoConfig#editorAjaxRequestHandlerExtensionPoint}.
 * </p>
 */
public class EditorAjaxRequestHandlerExtensionPointImpl
    extends ExtensionPoint_ImplBase<Request, EditorAjaxRequestHandler>
    implements EditorAjaxRequestHandlerExtensionPoint
{
    public EditorAjaxRequestHandlerExtensionPointImpl(
            @Lazy @Autowired(required = false) List<EditorAjaxRequestHandler> aExtensions)
    {
        super(aExtensions);
    }

    @Override
    public Optional<EditorAjaxRequestHandler> getHandler(Request aRequest)
    {
        return getExtensions().stream() //
                .filter(handler -> handler.accepts(aRequest)) //
                .findFirst();
    }

    @Override
    public List<EditorAjaxRequestHandler> getExtensions(Request aContext)
    {
        // EditorAjaxRequestHandler::accept may have side-effects! In particular the
        // ImplicitUnarmSlotHandler. You do not want this side effect to unarm a slot while
        // filtering the list of potentially matching extensions.
        // log.warn("EditorAjaxRequestHandler::accept may have side-effects! Do not use this
        // method.");
        // return super.getExtensions(aContext);
        throw new UnsupportedOperationException("Use getExtensions() instead!");
    }
}
