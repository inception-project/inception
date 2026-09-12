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
package de.tudarmstadt.ukp.inception.editor;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface AnnotationEditorRegistry
{
    List<AnnotationEditorFactory> getEditorFactories();

    AnnotationEditorFactory getEditorFactory(String aId);

    AnnotationEditorFactory getDefaultEditorFactory();

    AnnotationEditorFactory getPreferredEditorFactory(Project aProject, String aFormat);

    /**
     * Get the proper editor factory for the given format.
     *
     * @param aProject
     *            the project the document belongs to.
     * @param aFormat
     *            the format of the document to display.
     * @param aConfiguredId
     *            bean name of the explicitly configured editor, or {@code null} if none is
     *            configured. An unknown name is treated like {@code null}.
     * @return the editor factory to use. Never {@code null} as long as any editor is registered.
     */
    AnnotationEditorFactory getEditorFactory(Project aProject, String aFormat,
            String aConfiguredId);

    /**
     * @param aFactory
     *            the factory to check. May be {@code null}, which is never suitable.
     * @param aProject
     *            the project the document belongs to.
     * @param aFormat
     *            the format of the document to display.
     * @return whether the given factory can display documents of the given format, i.e. whether it
     *         reports anything other than {@link AnnotationEditorFactory#NOT_SUITABLE}.
     */
    boolean isSuitable(AnnotationEditorFactory aFactory, Project aProject, String aFormat);
}
