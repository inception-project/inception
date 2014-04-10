/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;


/**
 * Constants for annotation types
 * @author Seid Muhie Yimam
 *
 */
public class WebAnnoConst
{
    // Annotation types, for span or arc annotations.
    public static final String POS = "pos";
    public static final String NAMEDENTITY = "named entity";
    public static final String DEPENDENCY = "dependency";
    public static final String COREFERENCE = "coreference";
    public static final String COREFRELTYPE = "coreference type";
    public static final String LEMMA = "lemma";


    public static final String POS_PREFIX = "POS_";
    public static final String DEP_PREFIX = "DEP_";
    public static final String NAMEDENTITY_PREFIX = "Named Entity_";
    public static final String COREFERENCE_PREFIX = "COREF_";
    public static final String COREFRELTYPE_PREFIX = "COREFT_";

    public static final String PREFIX_SEPARATOR = "_";


    public static final String POS_PARENT = "POS";
    public static final String NAMEDENTITY_PARENT = "Named Entity";
    public static final String COREFERENCE_PARENT = "COREF";

    public static final String ROOT = "ROOT";

    public static final String SPAN_TYPE = "span";
    public static final String RELATION_TYPE = "relation";
    public static final String CHAIN_TYPE = "chain";

    public static final String COREFERENCE_RELATION_FEATURE = "referenceRelation";
    public static final String COREFERENCE_TYPE_FEATURE = "referenceType";




}
