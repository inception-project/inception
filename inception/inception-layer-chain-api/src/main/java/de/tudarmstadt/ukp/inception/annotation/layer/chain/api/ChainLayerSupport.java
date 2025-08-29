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

import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#chainLayerSupport}.
 * </p>
 */
public interface ChainLayerSupport
    extends LayerSupport<ChainAdapter, ChainLayerTraits>
{
    public static final String TYPE = WebAnnoConst.CHAIN_TYPE;

    public static final String FEATURE_NAME_FIRST = "first";
    public static final String FEATURE_NAME_NEXT = "next";
    public static final String FEATURE_NAME_REFERENCE_RELATION = "referenceRelation";
    public static final String FEATURE_NAME_REFERENCE = "referenceType";
    public static final String TYPE_SUFFIX_LINK = "Link";
    public static final String TYPE_SUFFIX_CHAIN = "Chain";
}
