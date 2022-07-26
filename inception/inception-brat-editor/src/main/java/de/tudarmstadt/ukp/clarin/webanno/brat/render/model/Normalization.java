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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "target", "refDb", "refId", "refText" })
public class Normalization
{
    private VID target;

    @JsonInclude(NON_NULL)
    private String refDb;

    @JsonInclude(NON_NULL)
    private String refId;

    // If the refText is set, no AJAX query is performed. This is a behavior modification in the
    // JS code by us
    // NOTE: If refText is set, then refId **MUST** also be set!
    @JsonInclude(NON_NULL)
    private String refText;

    public Normalization(VID aTarget, String aReftext)
    {
        this(aTarget, "", "", aReftext);
    }

    public Normalization(VID aTarget, String aRefDb, String aRefId)
    {
        this(aTarget, aRefDb, defaultIfEmpty(aRefId, null), null);
    }

    public Normalization(VID aTarget, String aRefDb, String aRefId, String aReftext)
    {
        target = aTarget;
        refText = aReftext;
        refDb = aRefDb;
        refId = aRefId;
    }

    public VID getTarget()
    {
        return target;
    }

    public void setTarget(VID aTarget)
    {
        target = aTarget;
    }

    public String getRefDb()
    {
        return refDb;
    }

    public void setRefDb(String aRefDb)
    {
        refDb = aRefDb;
    }

    public String getRefId()
    {
        return refId;
    }

    public void setRefId(String aRefId)
    {
        refId = aRefId;
    }

    public String getReftext()
    {
        return refText;
    }

    public void setReftext(String aReftext)
    {
        refText = aReftext;
    }
}
