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
package de.tudarmstadt.ukp.inception.diam.editor;

import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.inception.diam.model.DiamContext;

/**
 * The dispatch context for a DIAM AJAX request: the {@link Request} together with the
 * {@link DiamContext} of the editor that received it.
 * <p>
 * This is the {@code C} of the {@code Extension<C>} framework for
 * {@link de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler}. Bundling both
 * into a single value keeps the framework's single-context {@code accepts(C)} shape while still
 * giving acceptance checks access to the requesting editor's state (so they no longer fall back to
 * the main page).
 */
public class DiamRequest
{
    private final DiamContext context;
    private final Request request;

    public DiamRequest(DiamContext aContext, Request aRequest)
    {
        context = aContext;
        request = aRequest;
    }

    public DiamContext getContext()
    {
        return context;
    }

    public Request getRequest()
    {
        return request;
    }
}
