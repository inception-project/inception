/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public interface LayerBehavior
{
    /**
     * Checks whether the given layer behavior is supported for the given layer type.
     * 
     * @param aLayerType
     *            a layer support.
     * @return whether the given layer is provided by the current layer support.
     */
    boolean accepts(LayerSupport<?> aLayerType);
    
    /**
     * Check if all annotations of this layer conform with the behavior configuration. This is
     * usually called when a document is marked as finished to prevent invalid annotations ending up
     * in the finished document.
     */
    default List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, JCas aJCas)
    {
        return emptyList();
    }
}
