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

import java.util.List;
import java.util.function.Predicate;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface LayerSelectionState
{
    /**
     * @return annotation layer of currently selected annotation.
     */
    AnnotationLayer getSelectedAnnotationLayer();

    /**
     * @param aLayer
     *            annotation layer of currently selected annotation.
     */
    void setSelectedAnnotationLayer(AnnotationLayer aLayer);

    /**
     * @return annotation layer used for newly created annotations.
     */
    AnnotationLayer getDefaultAnnotationLayer();

    /**
     * @param aLayer
     *            annotation layer used for newly created annotations.
     */
    void setDefaultAnnotationLayer(AnnotationLayer aLayer);

    /**
     * Set all layers in the project, including hidden layers, non-enabled layers, etc. This is just
     * a convenience method to quickly access layer information and to avoid having to access the
     * database every time during rendering to get this information.
     * 
     * @param aLayers
     *            all layers.
     */
    void setAllAnnotationLayers(List<AnnotationLayer> aLayers);

    List<AnnotationLayer> getAllAnnotationLayers();

    /**
     * Get the annotation layers which are usable by the annotator (i.e. enabled, visible according
     * to the user preferences , etc.)
     * 
     * @return usable layers
     */
    List<AnnotationLayer> getAnnotationLayers();

    /**
     * Set the annotation layers which are usable by the annotator (i.e. enabled, visible according
     * to the user preferences , etc.)
     * 
     * @param aAnnotationLayers
     *            usable layers
     */
    void setAnnotationLayers(List<AnnotationLayer> aAnnotationLayers);

    void refreshSelectableLayers(Predicate<AnnotationLayer> isLayerBlocked);

    List<AnnotationLayer> getSelectableLayers();
}
