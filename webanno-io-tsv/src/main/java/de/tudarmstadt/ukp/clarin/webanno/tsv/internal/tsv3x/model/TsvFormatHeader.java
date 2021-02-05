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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import org.apache.commons.lang3.StringUtils;

public class TsvFormatHeader
{
    private final String name;
    private final String version;
    private final int majorVersion;
    private final int minorVersion;

    public TsvFormatHeader(String aName, String aVersion)
    {
        name = aName;
        version = aVersion;

        if (StringUtils.isNoneBlank(version)) {
            String[] parts = version.split("\\.");
            majorVersion = Integer.valueOf(parts[0]);
            minorVersion = Integer.valueOf(parts[1]);
        }
        else {
            majorVersion = 1;
            minorVersion = 0;
        }
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public int getMajorVersion()
    {
        return majorVersion;
    }

    public int getMinorVersion()
    {
        return minorVersion;
    }
}
