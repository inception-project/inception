/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/***
 * Serialized Class containing objects after parsing and creating objects based on rules file
 * 
 *
 */
public class ParsedConstraints
    implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = -2401965871743170805L;
    private final Map<String, String> imports;
    private final List<Scope> scopes;
    private Map<String, Scope> scopeMap = null;

    /**
     * @param imports
     * @param scopes
     */
    public ParsedConstraints(Map<String, String> imports, List<Scope> scopes)
    {
        this.imports = imports;
        this.scopes = scopes;
    }

    @Override
    public String toString()
    {
        return "Imports\n[" + printImports() + "]\n" + scopes.toString() + "]\n]";
    }

    private String printImports()
    {

        StringBuffer output = new StringBuffer();
        for (Entry<String, String> e : imports.entrySet()) {
            output.append(e.getKey());
            output.append(" is short for ");
            output.append(e.getValue());
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    public Map<String, String> getImports()
    {
        return imports;
    }

    public String getShortName(String aLongName)
    {
        for (Entry<String, String> e : imports.entrySet()) {
            if (e.getValue().equals(aLongName)) {
                return e.getKey();
            }
        }
        return null;
    }

    public List<Scope> getScopes()
    {
        return scopes;
    }

    public Scope getScopeByName(String scopeName)
    {

        if (scopeMap == null) { // initialize map if not set already
            scopeMap = new HashMap<String, Scope>();
            for (Scope scope : scopes) {
                scopeMap.put(scope.getScopeName(), scope);
            }
        }
        return scopeMap.get(scopeName);
    }

}
