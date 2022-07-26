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

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;

public class RenderAnnotationsEvent
{
    private final CAS cas;
    private final RenderRequest request;
    private final VDocument vdoc;

    public RenderAnnotationsEvent(CAS aCas, RenderRequest aRequest, VDocument aVDoc)
    {
        cas = aCas;
        request = aRequest;
        vdoc = aVDoc;
    }

    public CAS getCas()
    {
        return cas;
    }

    public RenderRequest getRequest()
    {
        return request;
    }

    public VDocument getVDocument()
    {
        return vdoc;
    }
}
