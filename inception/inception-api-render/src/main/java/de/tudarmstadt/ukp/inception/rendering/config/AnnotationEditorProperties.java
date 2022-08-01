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
package de.tudarmstadt.ukp.inception.rendering.config;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;

public interface AnnotationEditorProperties
{
    boolean isTokenLayerEditable();

    boolean isSentenceLayerEditable();

    /**
     * @return whether the "forward annotation" setting is available to annotators.
     * @deprecated to be removed without replacement
     */
    @Deprecated
    boolean isForwardAnnotationEnabled();

    /**
     * @return whether the "remember layer" setting is configurable by the user as preference. When
     *         disabled, {@link AnnotationPreference#setRememberLayer} is set to true.
     */
    boolean isRememberLayerEnabled();

    default boolean isLayerBlocked(AnnotationLayer aLayer)
    {
        if (!isTokenLayerEditable() && Token.class.getName().equals(aLayer.getName())) {
            return true;
        }

        if (!isSentenceLayerEditable() && Sentence.class.getName().equals(aLayer.getName())) {
            return true;
        }

        return false;
    }

    /**
     * @deprecated Configurable JavaScript action to be removed soon.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    boolean isConfigurableJavaScriptActionEnabled();
}
