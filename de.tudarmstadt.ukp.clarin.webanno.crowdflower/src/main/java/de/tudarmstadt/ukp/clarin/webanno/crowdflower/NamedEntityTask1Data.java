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
 * Represents the JSON data structure send to Crowdflower for NamedEntityTask1
 * @author Benjamin
 *
 */
public class NamedEntityTask1Data
{
    String text = "";
    String _golden = "";
    String markertext = "";
    String markertext_gold = "";
    String markertext_gold_reason = "";
    String types = "";

    // from http://crowdflower.com/docs-gold: The _difficulty column should contain an integer between 1 and 100. The higher the number, the more difficult the Gold is considered, and it will be displayed later in a contributor's judgment session.
    int _difficulty = 1;

    String document = "";

    @JsonProperty("document")
    public String getDocument()
    {
        return document;
    }

    public void setDocument(String document)
    {
        this.document = document;
    }

    @JsonProperty("offset")
    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }
    int offset = 0;

    //constructor for normal data
    NamedEntityTask1Data(String text)
    {
        this.text = text;
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

    @JsonProperty("_golden")
    public String get_golden()
    {
        return _golden;
    }
    public void set_golden(String _golden)
    {
        this._golden = _golden;
    }

    @JsonProperty("markertext")
    public String getMarkertext()
    {
        return markertext;
    }
    public void setMarkertext(String markertext)
    {
        this.markertext = markertext;
    }

    @JsonProperty("markertext_gold")
    public String getMarkertext_gold()
    {
        return markertext_gold;
    }
    public void setMarkertext_gold(String markertext_gold)
    {
        this.markertext_gold = markertext_gold;
    }

    @JsonProperty("markertext_gold_reason")
    public String getMarkertext_gold_reason()
    {
        return markertext_gold_reason;
    }
    public void setMarkertext_gold_reason(String markertext_gold_reason)
    {
        this.markertext_gold_reason = markertext_gold_reason;
    }

    @JsonProperty("types")
    public String getTypes()
    {
        return types;
    }

    public void setTypes(String types)
    {
        this.types = types;
    }

    @JsonProperty("_difficulty")
    public int get_difficulty()
    {
        return _difficulty;
    }

    public void set_difficulty(int _difficulty)
    {
        this._difficulty = _difficulty;
    }
}
