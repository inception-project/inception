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
 * Represents one data row of the JSON data structure send to Crowdflower for NamedEntityTask1
 * @author Benjamin Milde
 *
 */
public class NamedEntityTask2Data
{
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_TODECIDE = "toDecide";
    public static final String FIELD_GOLDEN = "_golden";
    public static final String FIELD_HIDDEN = "_hidden";
    public static final String FIELD_DIFFICULTY = "_difficulty";
    public static final String FIELD_TODECIDE_RESULT = "ist_todecide_eine";
    public static final String FIELD_TODECIDE_GOLD = "ist_todecide_eine_gold";
    public static final String FIELD_TODECIDE_GOLD_REASON = "ist_todecide_eine_gold_reason";
    public static final String FIELD_TOKENOFFSET = "tokenOffest";
    public static final String FIELD_DOCOFFSET = "docOffset";
    public static final String FIELD_DOCUMENT = "document";
    public static final String FIELD_POSTEXT = "posText";

    //context text displayed to worker
    private String text = "";

    //word for which NE type should be recognized
    private String toDecide = "";

    //is the element golden?
    private String _golden = "";

    //is this element hidden (default all elements are visible)
    private String _hidden = "FALSE";

    // from http://crowdflower.com/docs-gold: The _difficulty column should contain an integer between 1 and 100.
    // The higher the number, the more difficult the Gold is considered, and it will be displayed later in a contributor's judgment session.
    private int _difficulty = 1;

    //gold solution for this item, can be left empty if _golden is not TRUE
    private String ist_todecide_ein_gold = "";
    private String ist_todecide_ein_gold_reason = "";

    //token offset
    private String tokenOffset = "";

    //offset this document has for the start token (to allow contious numbering for multi-documents)
    private int docOffset = 0;

    //Document name
    private String document = "";

    //position JSON which contains start and end token for this NE
    private String posText = "";

    //constructor for normal data
    NamedEntityTask2Data(String text, String toDecide, String posText, String tokenOffset, String document)
    {
           this.text = text;
           this.toDecide = toDecide;

           this.tokenOffset = tokenOffset;
           this.document = document;
           this.posText = posText;
    }

    //constructor for gold data
    NamedEntityTask2Data(String text, String toDecide, String posText, String tokenOffset, String document, String ist_todecide_ein_gold, String ist_todecide_ein_gold_reason)
    {
           this.text = text;
           this.toDecide = toDecide;

           this.ist_todecide_ein_gold = ist_todecide_ein_gold;
           this.ist_todecide_ein_gold_reason = ist_todecide_ein_gold_reason;
           this._golden = "TRUE";

           this.tokenOffset = tokenOffset;
           this.document = document;
           this.posText = posText;
    }

    NamedEntityTask2Data()
    {

    }

    @JsonProperty(FIELD_TEXT)
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        this.text = text;
    }

    @JsonProperty(FIELD_TODECIDE)
    public String getToDecide()
    {
        return toDecide;
    }
    public void setToDecide(String toDecide)
    {
        this.toDecide = toDecide;
    }

    @JsonProperty(FIELD_GOLDEN)
    public String get_golden()
    {
        return _golden;
    }

    public void set_Golden(String _golden)
    {
        this._golden = _golden;
    }

    @JsonProperty(FIELD_HIDDEN)
    public String get_hidden()
    {
        return _hidden;
    }
    public void set_hidden(String _hidden)
    {
        this._golden = _hidden;
    }

    @JsonProperty(FIELD_TODECIDE_GOLD)
    public String get_ist_todecide_ein_gold()
    {
        return ist_todecide_ein_gold;
    }

    public void set_ist_todecide_ein_gold(String _ist_todecide_ein_gold)
    {
        this.ist_todecide_ein_gold = _ist_todecide_ein_gold;
    }

    @JsonProperty(FIELD_TODECIDE_GOLD_REASON)
    public String get_ist_todecide_ein_gold_reason()
    {
        return ist_todecide_ein_gold_reason;
    }

    public void set_ist_todecide_ein_gold_reason(String _ist_todecide_ein_gold_reason)
    {
        this.ist_todecide_ein_gold_reason = _ist_todecide_ein_gold_reason;
    }

    @JsonProperty(FIELD_TOKENOFFSET)
    public String getTokenOffset()
    {
        return tokenOffset;
    }

    public void setTokenOffset(String tokenOffset)
    {
        this.tokenOffset = tokenOffset;
    }

    @JsonProperty(FIELD_DOCUMENT)
    public String getDocument()
    {
        return document;
    }

    public void setDocument(String document)
    {
        this.document = document;
    }

    @JsonProperty(FIELD_POSTEXT)
    public String getPosText()
    {
        return posText;
    }

    public void setPosText(String posText)
    {
        this.posText = posText;
    }

    @JsonProperty(FIELD_DIFFICULTY)
    public int get_difficulty()
    {
        return _difficulty;
    }

    public void set_difficulty(int _difficulty)
    {
        this._difficulty = _difficulty;
    }

    @JsonProperty(FIELD_DOCOFFSET)
    public int getDocOffset()
    {
        return docOffset;
    }

    public void setDocOffset(int docOffset)
    {
        this.docOffset = docOffset;
    }
}
