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
package de.tudarmstadt.ukp.inception.pivot.api.report;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "layer", "feature" })
public class ExtractorDef
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String id;
    private String layer;
    private String feature;

    public ExtractorDef()
    {
    }

    public ExtractorDef(String aId, String aLayer, String aFeature)
    {
        id = aId;
        layer = aLayer;
        feature = aFeature;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public String getLayer()
    {
        return layer;
    }

    public void setLayer(String aLayer)
    {
        layer = aLayer;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }
        if (!(aOther instanceof ExtractorDef that)) {
            return false;
        }
        return Objects.equals(id, that.id) //
                && Objects.equals(layer, that.layer) //
                && Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, layer, feature);
    }

    @Override
    public String toString()
    {
        return "ExtractorDef[" + id + " @ " + layer + (feature != null ? "." + feature : "") + "]";
    }
}
