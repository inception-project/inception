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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public interface AnnotationEditorExtension
{
    /**
     * @return get the bean name.
     */
    String getBeanName();

    /**
     * Handle an action.
     * 
     * @param aActionHandler
     *            the action handler.
     * @param aState
     *            the annotator state
     * @param aTarget
     *            the AJAX target
     * @param aCas
     *            the CAS being edited
     * @param paramId
     *            the annotation the action is being performed on
     * @param aAction
     *            the action to perform
     * @throws AnnotationException
     *             if there was an annotation-level exception
     * @throws IOException
     *             if there was an I/O-level exception
     */
    default void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID paramId, String aAction)
        throws AnnotationException, IOException
    {
        // Do nothing by default
    }

    default void renderRequested(AjaxRequestTarget aTarget, AnnotatorState aState)
    {
        // Do nothing by default
    }

    default List<VLazyDetailGroup> lookupLazyDetails(SourceDocument aDocument, User aDataOwner,
            CAS aCas, VID aVid, AnnotationLayer aLayer)
    {
        return Collections.emptyList();
    }

    <V> V getFeatureValue(SourceDocument aDocument, User aUser, CAS aCas, VID aVid,
            AnnotationFeature aFeature)
        throws IOException;

}
