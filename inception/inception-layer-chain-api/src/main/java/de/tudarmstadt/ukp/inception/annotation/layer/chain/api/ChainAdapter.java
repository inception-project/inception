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
package de.tudarmstadt.ukp.inception.annotation.layer.chain.api;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

/**
 * Manage interactions with annotations on a chain layer.
 */
public interface ChainAdapter
    extends TypeAdapter
{
    public static final String CHAIN = "Chain";
    public static final String LINK = "Link";
    public static final String FEAT_FIRST = "first";
    public static final String FEAT_NEXT = "next";

    public static final String ARC_LABEL_FEATURE = WebAnnoConst.COREFERENCE_RELATION_FEATURE;
    public static final String SPAN_LABEL_FEATURE = WebAnnoConst.COREFERENCE_TYPE_FEATURE;

    String getChainTypeName();

    String getChainFirstFeatureName();

    String getLinkNextFeatureName();

    boolean isLinkedListBehavior();

    AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException;

    AnnotationFS handle(CreateRelationAnnotationRequest aRequest);

    Selection selectLink(AnnotationFS aAnn);

    Selection selectSpan(AnnotationFS aAnn);
}
