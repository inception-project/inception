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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommentMapping
{
    private static final Pattern PATTERN = Pattern //
            .compile("(?<TYPE>[a-zA-Z_][a-zA-Z0-9_\\-.]+)" + //
                    "[:](?<FEAT>[a-zA-Z][a-zA-Z0-9]+)");

    private static final String TYPE = "TYPE";
    private static final String FEAT = "FEAT";

    private final String type;
    private final String feature;
    private final Pattern pattern;
    private final String replacement;

    private Matcher matcher;
    private String value;

    /**
     * Jackson requires this constructor - even if it is private - do not use!
     */
    @SuppressWarnings("unused")
    private CommentMapping()
    {
        this(null, null, null, null);
    }

    @JsonCreator
    public CommentMapping(@JsonProperty("type") String aType,
            @JsonProperty("feature") String aFeature, @JsonProperty("match") String aMatch,
            @JsonProperty("replace") String aReplace)
    {
        type = aType;
        feature = aFeature;
        pattern = Pattern.compile(aMatch != null ? aMatch : ".*");
        replacement = aReplace;
    }

    public CommentMapping(@JsonProperty("type") String aType,
            @JsonProperty("feature") String aFeature)
    {
        this(aType, aFeature, null, null);
    }

    public String getType()
    {
        return type;
    }

    public String getFeature()
    {
        return feature;
    }

    public boolean matches(String aValue)
    {
        value = aValue;
        matcher = pattern.matcher(aValue);
        return matcher.matches();
    }

    public String apply()
    {
        return replacement != null ? matcher.replaceFirst(replacement) : value;
    }

    public static CommentMapping parse(String aValue)
    {
        Matcher m = PATTERN.matcher(aValue);

        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Illegal note mapping parameter format [" + aValue + "]");
        }

        return new CommentMapping(m.group(TYPE), m.group(FEAT));
    }
}
