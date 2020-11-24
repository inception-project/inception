/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

public class FormatConstants
{
    public static final String HEADER_LAYER_PREFIX_SEPARATOR = "=";
    public static final String HEADER_PREFIX_FORMAT = "#FORMAT" + HEADER_LAYER_PREFIX_SEPARATOR;
    public static final String HEADER_PREFIX_ROLE = "ROLE_";
    public static final String HEADER_FIELD_SEPARATOR = "|";
    public static final String HEADER_PREFIX_BASE_TYPE = "BT_";
    public static final String HEADER_PREFIX_CHAIN_LAYER = "#T_CH" + HEADER_LAYER_PREFIX_SEPARATOR;
    public static final String HEADER_PREFIX_RELATION_LAYER = "#T_RL"
            + HEADER_LAYER_PREFIX_SEPARATOR;
    public static final String HEADER_PREFIX_SPAN_LAYER = "#T_SP" + HEADER_LAYER_PREFIX_SEPARATOR;

    public static final String PREFIX_TEXT = "#Text=";

    public static final String FIELD_SEPARATOR = "\t";
    public static final char LINE_BREAK = '\n';
    public static final String NULL_VALUE = "*";
    public static final String NULL_COLUMN = "_";
    public static final String STACK_SEP = "|";
    public static final String SLOT_SEP = ";";

    public FormatConstants()
    {
        // TODO Auto-generated constructor stub
    }

}
