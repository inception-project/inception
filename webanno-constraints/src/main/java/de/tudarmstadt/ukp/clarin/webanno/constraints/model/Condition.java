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
import java.util.ArrayList;

/**
 * Class containing object representation for Condition in a rule.
 * 
 * @author aakash
 */
public class Condition
    implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 5229065580264733470L;
    private final String path;
    private final String value;

    public Condition(String aPath, String aValue)
    {
        path = aPath;
        value = aValue;
    }

    public String getPath()
    {
        return path;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "Condition [[" + path + "] = [" + value + "]]";
    }

    public boolean matches(ArrayList<String> listOfValues)
    {
        boolean doesItMatch = false;
        for (String input : listOfValues) {
            if (value.equals(input)) {
                doesItMatch = true;
                break;
            }

        }
        return doesItMatch;
    }
}
