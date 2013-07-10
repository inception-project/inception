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
package de.tudarmstadt.ukp.clarin.webanno.crowdflower;


import org.codehaus.jackson.annotate.JsonProperty;

/**
* Represents the JSON data structure send to Crowdflower for NamedEntityTask2
* @author Benjamin Milde
*/
public class NamedEntityTask2Data
{
    String text = "";
    String toDecide = "";
    String _golden = "";
    String _ist_todecide_ein = "";
    String ist_todecide_ein_gold = "";
    String ist_todecide_ein_gold_reason = "";

    //constructor for normal data
    NamedEntityTask2Data(String text, String toDecide)
    {
           this.text = text;
           this.toDecide = toDecide;
    }

    //constructor for gold data
    NamedEntityTask2Data(String text, String toDecide,String _ist_todecide_ein, String _ist_todecide_ein_gold, String _ist_todecide_ein_gold_reason)
    {
           this.text = text;
           this.toDecide = toDecide;
           this._ist_todecide_ein =  _ist_todecide_ein;
           this.ist_todecide_ein_gold = _ist_todecide_ein_gold;
           this.ist_todecide_ein_gold_reason = _ist_todecide_ein_gold_reason;
           this._golden = "TRUE";
    }

    NamedEntityTask2Data()
    {

    }

    @JsonProperty("text")
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        this.text = text;
    }

    @JsonProperty("toDecide")
    public String getToDecide()
    {
        return toDecide;
    }
    public void setToDecide(String toDecide)
    {
        this.toDecide = toDecide;
    }

    @JsonProperty("_golden")
    public String get_golden()
    {
        return _golden;
    }

    public void set_Golden(String _golden)
    {
        this._golden = _golden;
    }

    @JsonProperty("_ist_todecide_ein")
    public String get_ist_todecide_ein()
    {
        return _ist_todecide_ein;
    }

    public void set_ist_todecide_ein(String _ist_todecide_ein)
    {
        this._ist_todecide_ein = _ist_todecide_ein;
    }

    @JsonProperty("ist_todecide_ein_gold")
    public String get_ist_todecide_ein_gold()
    {
        return ist_todecide_ein_gold;
    }

    public void set_ist_todecide_ein_gold(String _ist_todecide_ein_gold)
    {
        this.ist_todecide_ein_gold = _ist_todecide_ein_gold;
    }

    @JsonProperty("ist_todecide_ein_gold_reason")
    public String get_ist_todecide_ein_gold_reason()
    {
        return ist_todecide_ein_gold_reason;
    }

    public void set_ist_todecide_ein_gold_reason(String _ist_todecide_ein_gold_reason)
    {
        this.ist_todecide_ein_gold_reason = _ist_todecide_ein_gold_reason;
    }
}
