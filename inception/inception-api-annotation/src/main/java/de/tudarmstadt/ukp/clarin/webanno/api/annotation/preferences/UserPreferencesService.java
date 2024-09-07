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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import java.io.IOException;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public interface UserPreferencesService
{
    /**
     * Set annotation preferences of users for a given project such as window size, annotation
     * layers,... reading from the file system.
     * 
     * @param aState
     *            The {@link AnnotatorState} that will be populated with preferences from the file
     * @param aSessionOwner
     *            The user for whom we need to read the preference (preferences are stored per user)
     * @throws IOException
     *             hum?
     */
    void loadPreferences(AnnotatorState aState, String aSessionOwner) throws IOException;

    AnnotationPreference loadPreferences(Project aProject, String aSessionOwner, Mode aMode)
        throws IOException;

    void savePreferences(AnnotatorState aState, String aSessionOwner) throws IOException;

    void savePreferences(Project aProject, String aSessionOwner, Mode aMode,
            AnnotationPreference aPref)
        throws IOException;
}
