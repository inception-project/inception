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
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public abstract class RelationLayerBehavior
    implements LayerBehavior
{
    @Override
    public boolean accepts(LayerSupport<?> aLayerType)
    {
        return aLayerType instanceof RelationLayerSupport;
    }

    public abstract CreateRelationAnnotationRequest onCreate(RelationAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException;

    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VArc> aAnnoToArcIdx)
    {
        // Nothing to do by default
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, CAS aCas)
    {
        return emptyList();
    }
}
