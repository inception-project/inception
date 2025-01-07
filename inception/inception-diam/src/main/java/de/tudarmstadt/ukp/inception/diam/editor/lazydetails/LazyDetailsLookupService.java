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
package de.tudarmstadt.ukp.inception.diam.editor.lazydetails;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.IRequestParameters;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public interface LazyDetailsLookupService
{
    List<LazyDetailGroup> lookupLazyDetails(IRequestParameters request, VID paramId,
            CasProvider aCas, SourceDocument aSourceDocument, User aDataOwner,
            int windowBeginOffset, int windowEndOffset)
        throws AnnotationException, IOException;

    List<VLazyDetailGroup> lookupAnnotationLevelDetails(VID aVid, SourceDocument aDocument,
            User aDataOwner, AnnotationLayer aLayer, CAS aCas)
        throws AnnotationException, IOException;

    List<VLazyDetailGroup> lookupFeatureLevelDetails(VID aVid, CAS aCas,
            AnnotationFeature aFeature);

    List<VLazyDetailGroup> lookupLayerLevelDetails(VID aVid, CAS aCas, AnnotationLayer aLayer);
}
