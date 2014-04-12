/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * A relation between span annotations -> an arc annotation. Example
 * "relations":[["d_48420","SUBJ",[["Arg1","p_21346"],["Arg2","p_21341"]]],...
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "id", "type", "arguments" })
public class Relation
{
    private int id;

    /**
     * The type of the relation between two spans
     */
    private String type;

    /**
     * The initial/destination span annotations as shown in the example above
     */
    List<Argument> arguments = new ArrayList<Argument>();

    public Relation()
    {
        // Nothing to do
    }

    public Relation(int aId, String aType, List<Argument> aArguments)
    {
        super();
        id = aId;
        type = aType;
        arguments = aArguments;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
    {
        id = aId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public List<Argument> getArguments()
    {
        return arguments;
    }

    public void setArguments(List<Argument> aArguments)
    {
        arguments = aArguments;
    }
}
