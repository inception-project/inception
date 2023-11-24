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
package de.tudarmstadt.ukp.inception.support.about;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MavenLicense
    implements Serializable
{
    private static final long serialVersionUID = -1899079347573697692L;
    private String name;
    private String url;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        if (isNotBlank(name)) {
            sb.append(name.trim());
        }
        if (isNotBlank(url)) {
            boolean addParentheses = sb.length() > 0;
            if (addParentheses) {
                sb.append(" (");
            }
            sb.append(url.trim());
            if (addParentheses) {
                sb.append(")");
            }
        }

        return sb.toString();
    }
}
