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
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "argument", "token" })
public class Argument
{
    /**
     * Arg1 or Arg2
     */
    private String argument;
    /**
     * the intial/target tokens (Span annotations)
     */
    private int token;

    public Argument()
    {
        // Nothing to do
    }

    public Argument(String aArgument, int aToken)
    {
        argument = aArgument;
        token = aToken;
    }

    public String getArgument()
    {
        return argument;
    }

    public void setArgument(String aArgument)
    {
        argument = aArgument;
    }

    public int getToken()
    {
        return token;
    }

    public void setTarget(int aTarget)
    {
        token = aTarget;
    }
}
