/*
 * Copyright 2018
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

package de.tudarmstadt.ukp.inception.conceptlinking.model;

/**
 * Describes a relation or property between two entities
 */
public class Property
{
    private String label;
    private String[] altlabel;
    private double freq;
    private String type;
        
    public Property(String label, String altlabel, String type, String freq)
    {
        super();
        this.label = label.trim().toLowerCase();
        this.altlabel = altlabel.trim().toLowerCase().split(", ");
        this.type = type;
        this.freq = Double.parseDouble(freq.trim().replace(",",""));
    }
    
    public String getLabel()
    {
        return label;
    }
    
    public void setLabel(String label)
    {
        this.label = label;
    }
    
    public String[] getAltlabel()
    {
        return altlabel;
    }

    public void setAltlabel(String[] altlabel)
    {
        this.altlabel = altlabel;
    }

    public double getFreq()
    {
        return freq;
    }
    
    public void setFreq(int freq)
    {
        this.freq = freq;
    }
    
    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
}
