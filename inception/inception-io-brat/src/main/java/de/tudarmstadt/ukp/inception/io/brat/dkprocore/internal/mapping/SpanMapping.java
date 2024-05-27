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

public class SpanMapping
{
    public static final String FLAG_ANCHOR = "A";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<TYPE>[a-zA-Z_][a-zA-Z0-9_\\-.]+)" + "(?:[:](?<SUBCAT>[a-zA-Z][a-zA-Z0-9]+))?");

    private static final String TYPE = "TYPE";
    private static final String SUBCAT = "SUBCAT";

    private final String type;
    private final String subcat;
    private final Map<String, String> defaultFeatureValues;

    /**
     * Jackson requires this constructor - even if it is private - do not use!
     */
    @SuppressWarnings("unused")
    private SpanMapping()
    {
        this(null, null);
    }

    @JsonCreator
    public SpanMapping(@JsonProperty(value = "type", required = true) String aType,
            @JsonProperty(value = "subCatFeature") String aSubCat,
            @JsonProperty(value = "defaultFeatureValues") Map<String, String> aDefaults)
    {
        type = aType;
        subcat = aSubCat;
        defaultFeatureValues = aDefaults != null ? aDefaults : Collections.emptyMap();
    }

    public SpanMapping(@JsonProperty(value = "type", required = true) String aType,
            @JsonProperty(value = "subCatFeature") String aSubCat)
    {
        this(aType, aSubCat, Collections.emptyMap());
    }

    public String getType()
    {
        return type;
    }

    public String getSubcat()
    {
        return subcat;
    }

    public Map<String, String> getDefaultFeatureValues()
    {
        return defaultFeatureValues;
    }

    public static SpanMapping parse(String aValue)
    {
        Matcher m = PATTERN.matcher(aValue);

        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Illegal text annotation parameter format [" + aValue + "]");
        }

        return new SpanMapping(m.group(TYPE), m.group(SUBCAT));
    }
}
