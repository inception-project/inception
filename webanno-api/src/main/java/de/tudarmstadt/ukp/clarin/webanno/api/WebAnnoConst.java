/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constants for annotation types
 */
public class WebAnnoConst
{
    // Page parameters
    public static final String PAGE_PARAM_PROJECT_ID = "p";
    public static final String PAGE_PARAM_PROJECT_NAME = "pn";
    public static final String PAGE_PARAM_DOCUMENT_ID = "d";
    public static final String PAGE_PARAM_DOCUMENT_NAME = "n";
    public static final String PAGE_PARAM_FOCUS = "f";

    // Annotation types, for span or arc annotations.
    public static final String POS = "pos";
    public static final String NAMEDENTITY = "named entity";
    public static final String DEPENDENCY = "dependency";
    public static final String COREFERENCE = "coreference";
    public static final String COREFRELTYPE = "coreference type";
    public static final String LEMMA = "lemma";
    
    public static final String FEAT_REL_TARGET = "Dependent";
    public static final String FEAT_REL_SOURCE = "Governor";

    public static final String SPAN_TYPE = "span";
    public static final String RELATION_TYPE = "relation";
    public static final String CHAIN_TYPE = "chain";

    public static final String COREFERENCE_RELATION_FEATURE = "referenceRelation";
    public static final String COREFERENCE_TYPE_FEATURE = "referenceType";

    public static final String TAB_SEP = "TAB-SEP";
    public static final String COREFERENCE_LAYER = "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference";
    public static final String MIRA = "/mira/";
    public static final String MIRA_TEMPLATE = "/template/";
    public static final String DOCUMENT = "/document/";
    public static final String TRAIN = "/train/";

    public static final String CURATION_USER = "CURATION_USER";
    public static final String CORRECTION_USER = "CORRECTION_USER";
    public static final String INITIAL_CAS_PSEUDO_USER = "INITIAL_CAS";
    
    public static final String PROJECT_TYPE_ANNOTATION = "annotation";
    public static final String PROJECT_TYPE_AUTOMATION = "automation";
    public static final String PROJECT_TYPE_CORRECTION = "correction";
    
    public static final List<String> RESTRICTED_FEATURE_NAMES = new ArrayList<String>(Arrays.asList(
            "address", "begin", "end", "coveredText", "booleanValue", "doubleValue", "byteValue",
            "CAS", "CASImpl", "class", "featureValue", "floatValue", "longValue", "lowLevelCas",
            "printRefs", "sofa", "stringValue", "type", "typeIndexId", "view"))
    {

        private static final long serialVersionUID = -8547798549706734147L;
        //Implement case-insensitive string comparison
        @Override
        public boolean contains(Object o)
        {

            String inputString = (String) o;
            for (String s : this) {
                if (inputString.equalsIgnoreCase(s)) {
                    return true;
                }
            }
            return false;
        }
    };

}
