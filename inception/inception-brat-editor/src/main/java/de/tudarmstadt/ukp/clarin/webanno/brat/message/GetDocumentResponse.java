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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Marker;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;

/**
 * Response for the {@code getDocument} command.
 */
public class GetDocumentResponse
    extends AjaxResponse
{
    public static final String COMMAND = "getDocument";

    @JsonProperty("rtl_mode")
    private boolean rtlMode;

    @JsonProperty("font_zoom")
    private int fontZoom;

    @JsonProperty("sentence_number_offset")
    private int sentenceNumberOffset;

    private String text;

    private int windowBegin;

    private int windowEnd;

    /**
     * [ 0, 3 ]
     */
    @JsonProperty("token_offsets")
    private List<Offsets> tokenOffsets = new ArrayList<>();

    /**
     * [ 0, 3 ]
     */
    @JsonProperty("sentence_offsets")
    private List<Offsets> sentenceOffsets = new ArrayList<>();

    private @JsonInclude(NON_EMPTY) List<Relation> relations = new ArrayList<>();

    /**
     * ["T1", "Protein", 8, 11]
     *
     * Guess: ID (maybe token ID?), Type, begin offset, end offset
     */
    private @JsonInclude(NON_EMPTY) List<Entity> entities = new ArrayList<>();
    private @JsonInclude(NON_EMPTY) List<Comment> comments = new ArrayList<>();

    private Map<String, List<Marker>> args = new HashMap<>();

    public GetDocumentResponse()
    {
        super(COMMAND);
    }

    public void addToken(int aBegin, int aEnd)
    {
        tokenOffsets.add(new Offsets(aBegin, aEnd));
    }

    public void addSentence(int aBegin, int aEnd)
    {
        sentenceOffsets.add(new Offsets(aBegin, aEnd));
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

    public List<Comment> getComments()
    {
        return comments;
    }

    public void setComments(List<Comment> aComments)
    {
        comments = aComments;
    }

    public void addComment(Comment aComment)
    {
        comments.add(aComment);
    }

    public List<Offsets> getTokenOffsets()
    {
        return tokenOffsets;
    }

    public void setTokenOffsets(List<Offsets> aTokenOffsets)
    {
        tokenOffsets = aTokenOffsets;
    }

    public List<Offsets> getSentenceOffsets()
    {
        return sentenceOffsets;
    }

    public void setSentenceOffsets(List<Offsets> aSentenceOffsets)
    {
        sentenceOffsets = aSentenceOffsets;
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public void setRelations(List<Relation> aRelations)
    {
        relations = aRelations;
    }

    public void addRelation(Relation aRelation)
    {
        relations.add(aRelation);
    }

    public List<Entity> getEntities()
    {
        return entities;
    }

    public void setEntities(List<Entity> aEntities)
    {
        entities = aEntities;
    }

    public void addEntity(Entity aEntity)
    {
        entities.add(aEntity);
    }

    public int getSentenceNumberOffset()
    {
        return sentenceNumberOffset;
    }

    public void setSentenceNumberOffset(int aSentenceNumberOffset)
    {
        sentenceNumberOffset = aSentenceNumberOffset;
    }

    public boolean isRtlMode()
    {
        return rtlMode;
    }

    public void setRtlMode(boolean aRtlMode)
    {
        rtlMode = aRtlMode;
    }

    public int getFontZoom()
    {
        return fontZoom;
    }

    public void setFontZoom(int aFontZoom)
    {
        fontZoom = aFontZoom;
    }

    public void addMarker(Marker aMarker)
    {
        List<Marker> markers = args.get(aMarker.getType());
        if (markers == null) {
            markers = new ArrayList<>();
            args.put(aMarker.getType(), markers);
        }
        markers.add(aMarker);
    }

    public Map<String, List<Marker>> getArgs()
    {
        return args;
    }

    public void setArgs(Map<String, List<Marker>> aArgs)
    {
        args = aArgs;
    }

    public void setWindowBegin(int aWindowBegin)
    {
        windowBegin = aWindowBegin;
    }

    public int getWindowBegin()
    {
        return windowBegin;
    }

    public void setWindowEnd(int aWindowEnd)
    {
        windowEnd = aWindowEnd;
    }

    public int getWindowEnd()
    {
        return windowEnd;
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
