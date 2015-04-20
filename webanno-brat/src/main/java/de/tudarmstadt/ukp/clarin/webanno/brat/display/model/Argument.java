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

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * The Arguments used during arc annotation in the form of [["Arg1","p_21346"],["Arg2","p_21341"]]
 * to denote a given arc annotation such as dependency parsing and coreference resolution
 *
 * @author Seid Muhie Yimam
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "label", "target" })
public class Argument
{
    /**
     * Argument label.
     */
    private final String label;

    /**
     * The target span annotation.
     */
    private final VID target;

    public Argument(String aLabel, int aTarget)
    {
        // REC: It is fully ok that we only have "int" as the type for target, since right now only
        // spans can be the target of an argument. However, we internally wrap this as a VID
        // in order to always use the same identifier type when talking with brat (VID renders as
        // as String value, while "int" would render as a numeric value).
        label = aLabel;
        target = new VID(aTarget);
    }

    public String getLabel()
    {
        return label;
    }

    public VID getTarget()
    {
        return target;
    }
}
