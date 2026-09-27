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

    private final AjaxRequestTarget requestHandler;

    private final boolean editorStructureAffected;

    /**
     * @param aProject
     *            the project the preferences apply to.
     * @param aSessionOwnerName
     *            the user whose preferences were changed.
     * @param aRequestHandler
     *            the current AJAX target, or {@code null} outside a partial page update.
     * @param aEditorStructureAffected
     *            whether a change was made that requires the editor component to be <b>rebuilt</b>
     *            rather than merely re-rendered - see {@link #isEditorStructureAffected()}.
     */
    public AnnotationPreferencesChangedEvent(Project aProject, String aSessionOwnerName,
            AjaxRequestTarget aRequestHandler, boolean aEditorStructureAffected)
    {
        project = aProject;
        sessionOwnerName = aSessionOwnerName;
        requestHandler = aRequestHandler;
        editorStructureAffected = aEditorStructureAffected;
    }

    /**
     * Convenience constructor for senders that cannot tell what changed and must therefore assume
     * the worst.
     *
     * @param aProject
     *            the project the preferences apply to.
     * @param aSessionOwnerName
     *            the user whose preferences were changed.
     * @param aRequestHandler
     *            the current AJAX target, or {@code null} outside a partial page update.
     */
    public AnnotationPreferencesChangedEvent(Project aProject, String aSessionOwnerName,
            AjaxRequestTarget aRequestHandler)
    {
        this(aProject, aSessionOwnerName, aRequestHandler, true);
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
     * @return the current AJAX target, or {@code null} if the change happened outside an AJAX
     *         request.
     */
    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }

    /**
     * @return whether the editor component must be rebuilt rather than re-rendered. Senders that
     *         cannot tell should say {@code true}: rebuilding when it was unnecessary costs the
     *         reading position, but re-rendering when a rebuild was needed leaves the editor
     *         disagreeing with the preferences it is supposed to follow.
     */
    public boolean isEditorStructureAffected()
    {
        return editorStructureAffected;
    }
}
