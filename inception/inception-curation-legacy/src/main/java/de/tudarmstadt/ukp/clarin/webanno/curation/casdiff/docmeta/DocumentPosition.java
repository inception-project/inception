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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.docmeta;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

/**
 * Represents a document position.
 */
public class DocumentPosition
    extends Position_ImplBase
{
    private static final long serialVersionUID = -1020728944030217843L;

    public DocumentPosition(String aCollectionId, String aDocumentId, String aType, String aFeature,
            String aRole, int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
            LinkFeatureMultiplicityMode aLinkCompareBehavior)
    {
        super(aCollectionId, aDocumentId, aType, aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd,
                aLinkTargetText, aLinkCompareBehavior);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Document [");
        toStringFragment(builder);
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toMinimalString()
    {
        return "Document";
    }
}
