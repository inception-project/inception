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
package de.tudarmstadt.ukp.inception.rendering.pipeline;

import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.serverTiming;
import static java.lang.System.currentTimeMillis;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.rendering.config.RenderingAutoConfig;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link RenderingAutoConfig#renderingPipeline}.
 * </p>
 */
public class RenderingPipelineImpl
    implements RenderingPipeline
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RenderStepExtensionPoint renderStepExtensionPoint;

    public RenderingPipelineImpl(RenderStepExtensionPoint aRenderStepExtensionPoint)
    {
        renderStepExtensionPoint = aRenderStepExtensionPoint;
    }

    @Override
    public VDocument render(RenderRequest aRequest)
    {
        LOG.trace("Rendering [{}-{}]", aRequest.getWindowBeginOffset(),
                aRequest.getWindowEndOffset());

        var vdoc = new VDocument();

        for (var step : renderStepExtensionPoint.getExtensions(aRequest)) {
            var start = currentTimeMillis();
            step.render(vdoc, aRequest);
            serverTiming("Rendering", "Rendering (" + step.getId() + ")",
                    currentTimeMillis() - start);
        }

        return vdoc;
    }
}
