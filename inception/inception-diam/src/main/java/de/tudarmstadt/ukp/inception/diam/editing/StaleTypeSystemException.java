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
package de.tudarmstadt.ukp.inception.diam.editing;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;

/**
 * Thrown when a feature configured on a layer does not exist in the type system of the CAS being
 * read. This happens when the layer definition was changed after the CAS was loaded, i.e. the
 * document needs to be re-opened to pick up the new type system.
 */
public class StaleTypeSystemException
    extends AnnotationException
{
    private static final long serialVersionUID = 7365398745092837465L;

    private final AnnotationFeature feature;

    public StaleTypeSystemException(AnnotationFeature aFeature)
    {
        super("The annotation typesystem seems to be out of date, try re-opening the document!");
        feature = aFeature;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }
}
