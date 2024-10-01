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
package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.io.Serializable;
import java.util.List;

/**
 * Class representing "Scope" (name) and list of "Rules" for a particular "Scope".
 */
public class Scope
    implements Serializable
{
    private static final long serialVersionUID = 226908916809455385L;

    private final String scopeName;
    private final List<Rule> rules;

    public Scope(String aScopeName, List<Rule> aRules)
    {
        scopeName = aScopeName;
        rules = aRules;
    }

    public String getScopeName()
    {
        return scopeName;
    }

    public List<Rule> getRules()
    {
        return rules;
    }

    @Override
    public String toString()
    {
        return "Scope [" + scopeName + "]\nRules\n" + rules.toString() + "]";
    }
}
