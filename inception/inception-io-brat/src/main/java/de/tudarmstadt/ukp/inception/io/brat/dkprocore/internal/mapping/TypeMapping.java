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

public class TypeMapping
{
    private static final Pattern PATTERN = Pattern
            .compile("(?<BRAT>.+?)" + "\\s*->\\s*" + "(?<UIMA>.+?)");

    private static final String BRAT = "BRAT";
    private static final String UIMA = "UIMA";

    private final Pattern bratTypePattern;
    private final String uimaType;
    private final Map<String, String> defaultFeatureValues;

    private Matcher matcher;

    public TypeMapping()
    {
        bratTypePattern = null;
        uimaType = null;
        defaultFeatureValues = null;
    }

    @JsonCreator
    public TypeMapping(@JsonProperty(value = "from", required = true) String aPattern,
            @JsonProperty(value = "to", required = true) String aReplacement,
            @JsonProperty(value = "defaultFeatureValues") Map<String, String> aDefaults)
    {
        bratTypePattern = Pattern.compile("^" + aPattern.trim() + "$");
        uimaType = aReplacement.trim();
        defaultFeatureValues = aDefaults != null ? aDefaults : Collections.emptyMap();
    }

    public TypeMapping(String aPattern, String aReplacement)
    {
        this(aPattern, aReplacement, Collections.emptyMap());
    }

    public boolean matches(String aType)
    {
        matcher = bratTypePattern.matcher(aType);
        return matcher.matches();
    }

    public String apply()
    {
        return matcher.replaceFirst(uimaType);
    }

    public Map<String, String> getDefaultFeatureValues()
    {
        return defaultFeatureValues;
    }

    public static TypeMapping parse(String aValue)
    {
        Matcher m = PATTERN.matcher(aValue);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal mapping parameter format [" + aValue + "]");
        }

        return new TypeMapping(m.group(BRAT), m.group(UIMA));
    }
}
