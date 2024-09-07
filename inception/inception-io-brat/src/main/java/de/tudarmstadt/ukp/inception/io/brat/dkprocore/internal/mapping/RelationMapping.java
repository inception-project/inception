/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RelationMapping
{
    public static final String FLAG_ANCHOR = "A";

    private static final Pattern PATTERN = Pattern.compile("(?<TYPE>[a-zA-Z_][a-zA-Z0-9_\\-.]+):"
            + "(?<ARG1>[a-zA-Z][a-zA-Z0-9]+)(?<FLAGS1>\\{A?\\})?:"
            + "(?<ARG2>[a-zA-Z][a-zA-Z0-9]+)(?<FLAGS2>\\{A?\\})?"
            + "(?:[:](?<SUBCAT>[a-zA-Z][a-zA-Z0-9]+))?");

    private static final String TYPE = "TYPE";
    private static final String ARG1 = "ARG1";
    private static final String FLAGS1 = "FLAGS1";
    private static final String ARG2 = "ARG2";
    private static final String FLAGS2 = "FLAGS2";
    private static final String SUBCAT = "SUBCAT";

    private final String type;
    private final String arg1;
    private final String flags1;
    private final String arg2;
    private final String flags2;
    private final String subcat;
    private final Map<String, String> defaultFeatureValues;

    /**
     * Jackson requires this constructor - even if it is private - do not use!
     */
    @SuppressWarnings("unused")
    private RelationMapping()
    {
        this(null, null, null, null, null, null);
    }

    @JsonCreator
    public RelationMapping(@JsonProperty(value = "type", required = true) String aType,
            @JsonProperty(value = "arg1", required = true) String aArg1,
            @JsonProperty(value = "flags1") String aFlags1,
            @JsonProperty(value = "arg2", required = true) String aArg2,
            @JsonProperty(value = "flags2") String aFlags2,
            @JsonProperty(value = "subCatFeature") String aSubCat,
            @JsonProperty(value = "defaultFeatureValues") Map<String, String> aDefaults)
    {
        type = aType;
        arg1 = aArg1;
        flags1 = aFlags1;
        arg2 = aArg2;
        flags2 = aFlags2;
        subcat = aSubCat;
        defaultFeatureValues = aDefaults != null ? aDefaults : Collections.emptyMap();
    }

    public RelationMapping(@JsonProperty(value = "type", required = true) String aType,
            @JsonProperty(value = "arg1", required = true) String aArg1,
            @JsonProperty(value = "flags1") String aFlags1,
            @JsonProperty(value = "arg2", required = true) String aArg2,
            @JsonProperty(value = "flags2") String aFlags2,
            @JsonProperty(value = "subCatFeature") String aSubCat)
    {
        this(aType, aArg1, aFlags1, aArg2, aFlags2, aSubCat, Collections.emptyMap());
    }

    public String getType()
    {
        return type;
    }

    public String getArg1()
    {
        return arg1;
    }

    public String getFlags1()
    {
        return flags1 != null ? flags1 : "";
    }

    public String getArg2()
    {
        return arg2;
    }

    public String getFlags2()
    {
        return flags2 != null ? flags2 : "";
    }

    public String getSubcat()
    {
        return subcat;
    }

    public Map<String, String> getDefaultFeatureValues()
    {
        return defaultFeatureValues;
    }

    public static RelationMapping parse(String aValue)
    {
        Matcher m = PATTERN.matcher(aValue);

        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Illegal relation parameter format [" + aValue + "]");
        }

        return new RelationMapping(m.group(TYPE), m.group(ARG1), m.group(FLAGS1), m.group(ARG2),
                m.group(FLAGS2), m.group(SUBCAT));
    }
}
