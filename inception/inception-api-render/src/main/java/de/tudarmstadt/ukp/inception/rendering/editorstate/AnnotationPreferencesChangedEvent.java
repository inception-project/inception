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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * Broadcast when the user's annotation preferences have been changed and saved, so that every
 * editor on the page can pick up the new settings.
 */
public class AnnotationPreferencesChangedEvent
{
    private final Project project;

    private final String sessionOwnerName;

    private final AnnotatorState source;

    private final AjaxRequestTarget requestHandler;

    /**
     * @param aProject
     *            the project the preferences apply to.
     * @param aSessionOwnerName
     *            the user whose preferences were changed.
     * @param aSource
     *            the editor state the preferences dialog was opened on and which has therefore
     *            already been updated in place, or {@code null} if unknown. Consumers holding this
     *            very state can skip reloading.
     * @param aRequestHandler
     *            the current AJAX target, or {@code null} outside a partial page update.
     */
    public AnnotationPreferencesChangedEvent(Project aProject, String aSessionOwnerName,
            AnnotatorState aSource, AjaxRequestTarget aRequestHandler)
    {
        project = aProject;
        sessionOwnerName = aSessionOwnerName;
        source = aSource;
        requestHandler = aRequestHandler;
    }

    public Project getProject()
    {
        return project;
    }

    /**
     * @return the user whose preferences were changed.
     */
    public String getSessionOwnerName()
    {
        return sessionOwnerName;
    }

    /**
     * @return the editor state that was already updated in place, or {@code null} if unknown. A
     *         consumer whose own state is this one has nothing to reload.
     */
    public AnnotatorState getSource()
    {
        return source;
    }

    /**
     * @return the current AJAX target, or {@code null} if the change happened outside an AJAX
     *         request.
     */
    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }

    /**
     * @param aState
     *            the editor state to test.
     * @return whether the given state already carries the new preferences, i.e. whether it is the
     *         one the dialog was opened on.
     */
    public boolean isAlreadyApplied(AnnotatorState aState)
    {
        return source != null && source == aState;
    }
}
