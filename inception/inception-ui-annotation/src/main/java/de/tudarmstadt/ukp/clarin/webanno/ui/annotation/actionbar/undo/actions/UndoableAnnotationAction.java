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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions;

import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostAction;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public interface UndoableAnnotationAction
{
    /**
     * Undo the captured action.
     * 
     * @param aSchemaService
     *            a schema service used to obtain an adapter.
     * @param aCas
     *            the editor CAS
     * @return an optional annotation ID to select after rolling back the captured action
     * @throws AnnotationException
     *             if the action cannot be rolled back
     */
    Optional<PostAction> undo(AnnotationSchemaService aSchemaService, CAS aCas)
        throws AnnotationException;
}
