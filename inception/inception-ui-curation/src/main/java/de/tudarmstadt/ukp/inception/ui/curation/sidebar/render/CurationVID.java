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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.render;

import static de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationEditorExtension.EXTENSION_ID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class CurationVID
    extends VID
{
    private static final long serialVersionUID = -4052847275637346338L;

    private final String username;

    public CurationVID(String aUsername, VID aVID)
    {
        super(aVID.getId(), EXTENSION_ID, aUsername + "!" + aVID.toString());
        username = aUsername;
    }

    public String getUsername()
    {
        return username;
    }

    @Override
    public String getExtensionPayload()
    {
        return StringUtils.substringAfter(super.getExtensionPayload(), "!");
    }

    /**
     * Parse extension payload of given VID into CurationVID
     */
    public static CurationVID parse(String aParamId)
    {
        // format of extension payload is <USER>!<VID> with standard VID format
        // <ID>-<SUB>.<ATTR>.<SLOT>@<LAYER>
        Matcher matcher = Pattern.compile("(?:(?<USER>[^!]+)\\!)(?<VID>.+)").matcher(aParamId);
        if (!matcher.matches()) {
            return null;
        }

        if (matcher.group("VID") == null || matcher.group("USER") == null) {
            return null;
        }

        String vidStr = matcher.group("VID");
        String username = matcher.group("USER");
        return new CurationVID(username, VID.parse(vidStr));
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof CurationVID)) {
            return false;
        }

        CurationVID castOther = (CurationVID) other;
        return new EqualsBuilder() //
                .append(username, castOther.username) //
                .append(getId(), castOther.getId()) //
                .append(getExtensionPayload(), castOther.getExtensionPayload()) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(username).append(getId()).append(getExtensionPayload())
                .toHashCode();
    }
}
