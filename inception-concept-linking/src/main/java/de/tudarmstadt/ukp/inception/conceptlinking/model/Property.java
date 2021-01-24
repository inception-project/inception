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

package de.tudarmstadt.ukp.inception.conceptlinking.model;

import java.util.Locale;

/**
 * Describes a relation or property between two entities# Source:
 * https://www.wikidata.org/wiki/Wikidata:Database_reports/List_of_properties/all
 */
public class Property
{
    private String label;
    private String[] altlabel;
    private int freq;
    private String type;

    public Property(String label, String altlabel, String type, String freq)
    {
        super();
        this.label = label.trim().toLowerCase(Locale.ENGLISH);
        this.altlabel = altlabel.trim().toLowerCase(Locale.ENGLISH).split(", ");
        this.type = type;
        this.freq = Integer.parseInt(freq.trim().replace(",", ""));
    }

    /**
     * @return The main label of this property
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label
     *            The main label of this property
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * @return Array of aliases for this property
     */
    public String[] getAltlabel()
    {
        return altlabel;
    }

    /**
     * @param altlabel
     *            aliases for this property
     */
    public void setAltlabel(String[] altlabel)
    {
        this.altlabel = altlabel;
    }

    /**
     * @return Frequency of usage
     */
    public int getFreq()
    {
        return freq;
    }

    /**
     * @param freq
     *            Frequency of usage
     */
    public void setFreq(int freq)
    {
        this.freq = freq;
    }

    /**
     * @return Wikidata data type, i.e. wikibase-item
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type
     *            Wikidata data type, i.e. wikibase-item
     */
    public void setType(String type)
    {
        this.type = type;
    }

}
