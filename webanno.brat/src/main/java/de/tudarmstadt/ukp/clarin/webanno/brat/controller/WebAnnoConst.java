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

    public static final String ROOT = "ROOT";

    public static final String SPAN_TYPE = "span";
    public static final String RELATION_TYPE = "relation";
    public static final String CHAIN_TYPE = "chain";

    public static final String COREFERENCE_RELATION_FEATURE = "referenceRelation";
    public static final String COREFERENCE_TYPE_FEATURE = "referenceType";

    public static final String TAB_SEP ="TAB-SEP";
    public static final String COREFERENCE_LAYER = "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference";

    public static final String CURATION_USER = "CURATION_USER";
    public static final String CORRECTION_USER = "CORRECTION_USER";
}
