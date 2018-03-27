/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.graph;

public class KBConceptProperty
{
    private String aProperty;
    private boolean mandatory;
    private boolean allowMultiple;

    public String getaProperty()
    {
        return aProperty;
    }

    public void setaProperty(String aAProperty)
    {
        aProperty = aAProperty;
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory(boolean aMandatory)
    {
        mandatory = aMandatory;
    }

    public boolean isAllowMultiple()
    {
        return allowMultiple;
    }

    public void setAllowMultiple(boolean aAllowMultiple)
    {
        allowMultiple = aAllowMultiple;
    }
}
