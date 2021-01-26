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
package de.tudarmstadt.ukp.inception.tageditor.brat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class MappingParam
{
    public static final String SEP = "->";

    private final Pattern pattern;
    private final String replacement;
    
    private Matcher matcher;
    
    public MappingParam(String aPattern, String aReplacement)
    {
        super();
        pattern = Pattern.compile("^" + aPattern.trim() + "$");
        replacement = aReplacement.trim();
    }
    
    public boolean matches(String aType)
    {
        matcher = pattern.matcher(aType);
        return matcher.matches();
    }
    
    public String apply()
    {
        return matcher.replaceFirst(replacement);
    }
    
    public static MappingParam parse(String aMapping)
    {
        int sep = StringUtils.lastIndexOf(aMapping, SEP);
        return new MappingParam(aMapping.substring(0, sep), aMapping.substring(sep + SEP.length()));
    }
}
