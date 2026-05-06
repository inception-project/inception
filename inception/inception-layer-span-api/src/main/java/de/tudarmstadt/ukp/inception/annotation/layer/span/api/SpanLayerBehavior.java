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
package de.tudarmstadt.ukp.inception.annotation.layer.span.api;

import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;

public abstract class SpanLayerBehavior
    implements LayerBehavior
{
    @Override
    public boolean accepts(LayerSupport<?, ?> aLayerType)
    {
        return aLayerType instanceof SpanLayerSupport;
    }

    public abstract CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException;

    public abstract MoveSpanAnnotationRequest onMove(TypeAdapter aAdapter,
            MoveSpanAnnotationRequest aRequest)
        throws AnnotationException;

    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VSpan> annoToSpanIdx)
    {
        // Nothing to do by default
    }
}
