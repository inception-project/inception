/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.core.app;

public class NameUtil
{
    /**
     * Check if the name is valid, SPecial characters are not allowed as a project/user name as it
     * will conflict with file naming system
     * 
     * @param aName
     *            a name.
     * @return if the name is valid.
     */
    public static boolean isNameValid(String aName)
    {
        if (aName.contains("^") || aName.contains("/") || aName.contains("\\")
                || aName.contains("&") || aName.contains("*") || aName.contains("?")
                || aName.contains("+") || aName.contains("$") || aName.contains("!")
                || aName.contains("[") || aName.contains("]")) {
            return false;
        }
        else {
            return true;
        }
    }
}
