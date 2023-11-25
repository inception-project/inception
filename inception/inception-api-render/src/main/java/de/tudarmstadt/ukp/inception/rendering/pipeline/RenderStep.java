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

import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface RenderStep
    extends Extension<RenderRequest>
{
    public static final int RENDER_STRUCTURE = 100;
    public static final int RENDER_SYNTHETIC_STRUCTURE = 200;
    public static final int RENDER_NOTIFICATION = 300;
    public static final int RENDER_FOCUS = 400;
    public static final int RENDER_LABELS = 500;
    public static final int RENDER_COLORS = 600;

    @Override
    default boolean accepts(RenderRequest aContext)
    {
        return true;
    }

    void render(VDocument aVdoc, RenderRequest aRequest);
}
