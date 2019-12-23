/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/*
 *           var id = norm[0];
          var normType = norm[1];
          var target = norm[2];
          var refdb = norm[3];
          var refid = norm[4];
          var reftext = norm[5];

 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "id", "type", "target", "refDb", "refId", "refText" })
public class Normalization
{
    public static final String TYPE_NAME = "name";
    public static final String TYPE_ATTR = "attr";
    public static final String TYPE_INFO = "info";
    
    // The UI JS code used the ID is only used to generate an error message if the target is missing
    // and otherwise this ID doesn't matter
    private String id;
    // The type seems to be entirely unused in the JS code 
    private String type; 
    private VID target;
    private String refDb;
    private String refId;
    // If the refText is set, no AJAX query is performed. This is a behavior modification in the
    // JS code by us
    private String refText;

    public Normalization(VID aTarget, String aReftext)
    {
        id = aTarget.toString();
        target = aTarget;
        type = TYPE_INFO;
        refText = aReftext;
    }
    
    public Normalization(VID aTarget, String aRefDb, String aRefId)
    {
        id = aTarget.toString();
        target = aTarget;
        type = TYPE_INFO;
        refDb = aRefDb;
        refId = aRefId;
    }

    public Normalization(VID aTarget, String aRefDb, String aRefId, String aReftext)
    {
        id = aTarget.toString();
        target = aTarget;
        type = TYPE_INFO;
        refText = aReftext;
        refDb = aRefDb;
        refId = aRefId;
    }

    public Normalization(String aId, VID aTarget, String aType, String aRefDb, String aRefId,
            String aReftext)
    {
        id = aId;
        target = aTarget;
        type = aType;
        refDb = aRefDb;
        refId = aRefId;
        refText = aReftext;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public VID getTarget()
    {
        return target;
    }

    public void setTarget(VID aTarget)
    {
        target = aTarget;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
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
