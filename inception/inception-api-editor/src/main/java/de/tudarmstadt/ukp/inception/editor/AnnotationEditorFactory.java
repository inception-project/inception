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

import static java.lang.Integer.MIN_VALUE;

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

public interface AnnotationEditorFactory
{
    static int NOT_SUITABLE = MIN_VALUE;
    static int DEFAULT = 1_000;
    static int PREFERRED = 10_000;

    /**
     * @return get the bean name.
     */
    String getBeanName();

    /**
     * @return the name of the editor as shown to the user.
     */
    String getDisplayName();

    default int accepts(Project aProject, String aFormat)
    {
        return DEFAULT;
    }

    AnnotationEditorBase create(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider);

    /**
     * Configure the state to be compatible with the editor produced by this factory. E.g. set the
     * paging strategy adequately.
     * 
     * @param aState
     *            the annotator state
     */
    void initState(AnnotatorState aState);

    Optional<PolicyCollection> getPolicy() throws IOException;
}
